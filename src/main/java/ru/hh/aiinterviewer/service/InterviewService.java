package ru.hh.aiinterviewer.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageResponseDto;
import ru.hh.aiinterviewer.domain.model.MessageTrigger;
import ru.hh.aiinterviewer.domain.model.MessageType;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionMode;
import ru.hh.aiinterviewer.domain.model.SessionStatus;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.exception.InvalidStatusTransitionException;
import ru.hh.aiinterviewer.exception.SessionCompletedException;
import ru.hh.aiinterviewer.exception.VacancyNotParsableException;
import ru.hh.aiinterviewer.llm.Prompts;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private static final int MAX_ITERATIONS = 100;
  private static final String EXCEEDED_LIMIT_MESSAGE = MessageTrigger.COMPLETE.getValue() + ". Превышен технический лимит по кол-ву сообщений :(";

  private final VacancyService vacancyService;
  private final SessionRepository sessionRepository;
  private final ChatClient interviewerChatClient;
  private final ChatClient prepareInterviewPlanChatClient;
  private final TranscriptionService transcriptionService;

  public String extractVacancyTextFromFile(MultipartFile file) {
    return vacancyService.extractTextFromFile(file);
  }

  @Transactional
  public UUID createSession(CreateSessionRequestDto request) {
    String vacancy;
    if (request.getMode() == SessionMode.ROLE) {
      if (request.getRoleName() == null || request.getRoleName().isBlank()) {
        throw new IllegalArgumentException("roleName must be provided when mode=role");
      }
      vacancy = request.getRoleName();
    } else {
      // mode=vacancy: one of vacancyUrl or vacancyText should be provided
      if (request.getVacancyText() != null && !request.getVacancyText().isBlank()) {
        vacancy = request.getVacancyText();
      } else if (request.getVacancyUrl() != null && !request.getVacancyUrl().isBlank()) {
        vacancy = vacancyService.getVacancyByUrl(request.getVacancyUrl());
      } else {
        throw new VacancyNotParsableException("One of vacancyUrl or vacancyText must be provided");
      }
    }

    String interviewPlan = prepareInterviewPlanChatClient
        .prompt()
        .user(Prompts.getPrepareInterviewPlanPrompt(vacancy, request.getNumQuestions(), request.getPlanPreferences()))
        .call()
        .content();

    Session session = createSession(request, interviewPlan);

    return session.getId();
  }

  private Session createSession(CreateSessionRequestDto request, String interviewPlan) {
    Session session = sessionRepository.save(Session.builder()
        .vacancyUrl(request.getVacancyUrl())
        .mode(request.getMode())
        .roleName(request.getRoleName())
        .status(SessionStatus.PLANNED)
        .numQuestions(request.getNumQuestions())
        .interviewPlan(interviewPlan)
        .planPreferences(request.getPlanPreferences())
        .interviewFormat(request.getInterviewFormat())
        .communicationStylePreset(request.getCommunicationStylePreset())
        .communicationStyleFreeform(request.getCommunicationStyleFreeform())
        .build());
    sessionRepository.flush();
    return session;
  }

  @Transactional
  public MessageResponseDto processMessage(UUID sessionId, MessageRequestDto userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    if (session.isCompleted()) {
      throw new SessionCompletedException("Session is already completed");
    }

    String assistantAnswer;

    if (session.getMessages().size() >= MAX_ITERATIONS) {
      session.completeInterview();
      String feedback = performChatInteraction(session, EXCEEDED_LIMIT_MESSAGE);
      sessionRepository.save(session);
      return buildFeedbackMessageResponse(session, feedback);
    }

    String userTextMessage = switch (MessageType.fromValue(userMessage.getType())) {
      case TEXT -> userMessage.getMessage();
      case AUDIO -> transcriptionService.transcribe(userMessage.getAudioBase64());
    };

    // State machine
    if (session.getStatus() == SessionStatus.PLANNED) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        session.startInterview();
        assistantAnswer = performChatInteraction(session, userTextMessage);
        sessionRepository.save(session);
        return buildNextMessageResponse(session, assistantAnswer);
      } else if (MessageTrigger.FEEDBACK.isTrigger(userTextMessage) || MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in planned status");
      } else {
        // Plan correction mode: keep PLANNED, regenerate plan
        String correctedPlan = prepareInterviewPlanChatClient
            .prompt()
            .system(Prompts.getRevisePlanPrompt(session.getInterviewPlan(), userTextMessage, session.getNumQuestions(), session.getPlanPreferences()))
            .call()
            .content();
        session.setInterviewPlan(correctedPlan);
        assistantAnswer = correctedPlan;
        sessionRepository.save(session);
        return buildNextMessageResponse(session, assistantAnswer);
      }
    } else if (session.getStatus() == SessionStatus.ONGOING) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in ongoing status");
      }
      if (MessageTrigger.FEEDBACK.isTrigger(userTextMessage)) {
        session.setStatus(SessionStatus.FEEDBACK);
        assistantAnswer = interviewerChatClient.prompt()
            .system(Prompts.getFeedbackPrompt(session.getInterviewPlan(), session.getCommunicationStylePreset()))
            .user(userTextMessage)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
            .call()
            .content();
        sessionRepository.save(session);
        return buildNextMessageResponse(session, assistantAnswer);
      }
      if (MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        session.completeInterview();
        assistantAnswer = performChatInteraction(session, MessageTrigger.COMPLETE.getValue());
        sessionRepository.save(session);
        return buildFeedbackMessageResponse(session, assistantAnswer);
      }
      assistantAnswer = performChatInteraction(session, userTextMessage);
      if (MessageTrigger.COMPLETE.isTrigger(assistantAnswer)) {
        session.completeInterview();
        sessionRepository.save(session);
        return buildFeedbackMessageResponse(session, assistantAnswer);
      }
    } else if (session.getStatus() == SessionStatus.FEEDBACK) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in feedback status");
      }
      if (MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        session.completeInterview();
        assistantAnswer = performChatInteraction(session, MessageTrigger.COMPLETE.getValue());
        sessionRepository.save(session);
        return buildFeedbackMessageResponse(session, assistantAnswer);
      }
      assistantAnswer = performChatInteraction(session, userTextMessage);
    } else if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new SessionCompletedException("Session is already completed");
    } else {
      throw new InvalidStatusTransitionException("Unsupported session status: " + session.getStatus());
    }

    sessionRepository.save(session);

    return buildNextMessageResponse(session, assistantAnswer);
  }

  public SseEmitter processMessageStream(UUID sessionId, MessageRequestDto userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    if (session.isCompleted()) {
      throw new SessionCompletedException("Session is already completed");
    }

    if (session.getMessages().size() >= MAX_ITERATIONS) {
      session.completeInterview();
      sessionRepository.save(session);
      return performChatInteractionStreaming(session, EXCEEDED_LIMIT_MESSAGE);
    }

    String userTextMessage = switch (MessageType.fromValue(userMessage.getType())) {
      case TEXT -> userMessage.getMessage();
      case AUDIO -> transcriptionService.transcribe(userMessage.getAudioBase64());
    };

    if (session.getStatus() == SessionStatus.PLANNED) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        session.startInterview();
      } else if (MessageTrigger.FEEDBACK.isTrigger(userTextMessage) || MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in planned status");
      } else {
        String correctedPlan = prepareInterviewPlanChatClient
            .prompt()
            .system(Prompts.getRevisePlanPrompt(session.getInterviewPlan(), userTextMessage, session.getNumQuestions(), session.getPlanPreferences()))
            .call()
            .content();
        session.setInterviewPlan(correctedPlan);
      }
      sessionRepository.save(session);
      return performChatInteractionStreaming(session, userTextMessage);
    } else if (session.getStatus() == SessionStatus.ONGOING) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in ongoing status");
      }
      if (MessageTrigger.FEEDBACK.isTrigger(userTextMessage)) {
        session.setStatus(SessionStatus.FEEDBACK);
      } else if (MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        session.completeInterview();
      }
      sessionRepository.save(session);
      return performChatInteractionStreaming(session, userTextMessage);
    } else if (session.getStatus() == SessionStatus.FEEDBACK) {
      if (MessageTrigger.START.isTrigger(userTextMessage)) {
        throw new InvalidStatusTransitionException("Command is not allowed in feedback status");
      }
      if (MessageTrigger.FINISH.isTrigger(userTextMessage)) {
        session.completeInterview();
      }
      sessionRepository.save(session);
      return performChatInteractionStreaming(session, userTextMessage);
    } else if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new SessionCompletedException("Session is already completed");
    } else {
      throw new InvalidStatusTransitionException("Unsupported session status: " + session.getStatus());
    }
  }

  private String performChatInteraction(Session session, String userMessage) {
    return interviewerChatClient.prompt()
        .system(Prompts.getInterviewerPrompt(
            session.getInterviewPlan(),
            session.getCommunicationStylePreset(),
            session.getInterviewFormat() == null ? null : session.getInterviewFormat().getValue()
        ))
        .user(userMessage)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
        .call()
        .content();
  }

  private SseEmitter performChatInteractionStreaming(Session session, String userMessage) {
    final StringBuilder answerBuilder = new StringBuilder();
    SseEmitter sseEmitter = new SseEmitter(0L);

    interviewerChatClient.prompt()
        .system(Prompts.getInterviewerPrompt(
            session.getInterviewPlan(),
            session.getCommunicationStylePreset(),
            session.getInterviewFormat() == null ? null : session.getInterviewFormat().getValue()
        ))
        .user(userMessage)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
        .stream()
        .chatResponse()
        .subscribe(
            (ChatResponse response) -> processToken(response, answerBuilder, sseEmitter),
            sseEmitter::completeWithError,
            () -> {
              onAnswerComplete(session, answerBuilder.toString());
              sseEmitter.complete();
            }
        );

    return sseEmitter;
  }

  @SneakyThrows
  private void processToken(ChatResponse response, StringBuilder answerBuilder, SseEmitter emitter) {
    var token = response.getResult().getOutput();
    String text = token.getText();
    if (text != null) {
      answerBuilder.append(text);
      emitter.send(text);
    }
  }

  @SneakyThrows
  private void onAnswerComplete(Session session, String answer) {
    if (MessageTrigger.COMPLETE.isTrigger(answer)) {
      session.completeInterview();
      sessionRepository.save(session);
    }
  }

  private MessageResponseDto buildNextMessageResponse(Session session, String message) {
    return MessageResponseDto.builder()
        .sessionId(session.getId().toString())
        .message(message)
        .interviewComplete(false)
        .build();
  }

  private MessageResponseDto buildFeedbackMessageResponse(Session session, String feedback) {
    return MessageResponseDto.builder()
        .sessionId(session.getId().toString())
        .message(feedback)
        .interviewComplete(true)
        .build();
  }
}
