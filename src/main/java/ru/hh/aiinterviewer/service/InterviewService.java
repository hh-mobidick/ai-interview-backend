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
import ru.hh.aiinterviewer.api.dto.CreateSessionRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageResponseDto;
import ru.hh.aiinterviewer.domain.model.MessageTrigger;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionStatus;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.llm.Prompts;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private static final int MAX_ITERATIONS = 100;

  private final VacancyService vacancyService;
  private final SessionRepository sessionRepository;
  private final ChatClient interviewerChatClient;
  private final ChatClient prepareInterviewPlanChatClient;

  @Transactional
  public UUID createSession(CreateSessionRequestDto request) {
    String vacancy = vacancyService.getVacancy(request.getVacancyUrl());

    String interviewPlan = prepareInterviewPlanChatClient
        .prompt()
        .user(Prompts.getPrepareInterviewPlanPrompt(vacancy, request.getNumQuestions(), request.getInstructions()))
        .call()
        .content();

    Session session = sessionRepository.save(Session.builder()
        .vacancyUrl(request.getVacancyUrl())
        .status(SessionStatus.PLANNED)
        .numQuestions(request.getNumQuestions())
        .interviewPlan(interviewPlan)
        .instructions(request.getInstructions())
        .communicationStyle(request.getCommunicationStyle())
        .build());
    sessionRepository.flush();

    interviewerChatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
        .system(Prompts.getInterviewerPrompt(interviewPlan, request.getCommunicationStyle()))
        .user(MessageTrigger.PLAN.getValue())
        .call()
        .content();

    return session.getId();
  }

  @Transactional
  public MessageResponseDto processMessage(UUID sessionId, String userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    session.ensureNotCompleted();

    if (session.getStatus() == SessionStatus.PLANNED) {
      return startInterview(session, userMessage);
    }

    if (session.getMessages().size() >= MAX_ITERATIONS) {
      String feedback = performChatInteraction(session, MessageTrigger.COMPLETE.getValue());
      completedInterview(session);
      return buildFeedbackMessageResponse(session, feedback);
    }

    String assistantAnswer = performChatInteraction(session, userMessage);

    if (MessageTrigger.COMPLETE.isTrigger(assistantAnswer)) {
      completedInterview(session);
      return buildFeedbackMessageResponse(session, assistantAnswer);
    }

    return buildNextQuestionMessageResponse(session, assistantAnswer);
  }

  public SseEmitter processMessageStream(UUID sessionId, String userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    session.ensureNotCompleted();

    if (session.getStatus() == SessionStatus.PLANNED) {
      return startInterviewStreaming(session, userMessage);
    }

    if (session.getMessages().size() >= MAX_ITERATIONS) {
      return forceCompleteInterviewStreaming(session);
    }

    return performChatInteractionStreaming(session, userMessage);
  }

  private MessageResponseDto startInterview(Session session, String userMessage) {
    MessageTrigger startTrigger = MessageTrigger
        .of(userMessage)
        .orElse(null);
    if (!MessageTrigger.START.equals(startTrigger)) {
      throw new IllegalStateException("To start interview, send 'Начать интервью'");
    }
    session.startInterview();

    String assistantAnswer = performChatInteraction(session, userMessage);

    sessionRepository.save(session);

    return buildNextQuestionMessageResponse(session, assistantAnswer);
  }

  private SseEmitter startInterviewStreaming(Session session, String userMessage) {
    MessageTrigger startTrigger = MessageTrigger
        .of(userMessage)
        .orElse(null);
    if (!MessageTrigger.START.equals(startTrigger)) {
      throw new IllegalStateException("To start interview, send 'Начать интервью'");
    }
    session.startInterview();

    sessionRepository.save(session);

    return performChatInteractionStreaming(session, userMessage);
  }

  private void completedInterview(Session session) {
    session.completeInterview();
    sessionRepository.save(session);
  }

  private SseEmitter forceCompleteInterviewStreaming(Session session) {
    session.completeInterview();
    sessionRepository.save(session);
    return performChatInteractionStreaming(session, MessageTrigger.COMPLETE.getValue());
  }

  private String performChatInteraction(Session session, String userMessage) {
    return interviewerChatClient.prompt()
        .system(Prompts.getInterviewerPrompt(session.getInterviewPlan(), session.getCommunicationStyle()))
        .user(userMessage)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
        .call()
        .content();
  }

  private SseEmitter performChatInteractionStreaming(Session session, String userMessage) {
    final StringBuilder answerBuilder = new StringBuilder();
    SseEmitter sseEmitter = new SseEmitter(0L);

    interviewerChatClient.prompt()
        .system(Prompts.getInterviewerPrompt(session.getInterviewPlan(), session.getCommunicationStyle()))
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
    if (token.getText() != null) {
      answerBuilder.append(token.getText());
      emitter.send(token.getText());
    }
  }

  @SneakyThrows
  private void onAnswerComplete(Session session, String answer) {
    if (MessageTrigger.COMPLETE.isTrigger(answer)) {
      completedInterview(session);
    }
  }

  private MessageResponseDto buildNextQuestionMessageResponse(Session session, String question) {
    return MessageResponseDto.builder()
        .sessionId(session.getId().toString())
        .message(question)
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
