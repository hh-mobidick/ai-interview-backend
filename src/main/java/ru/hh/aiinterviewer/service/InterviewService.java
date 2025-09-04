package ru.hh.aiinterviewer.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequest;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.MessageResponse;
import ru.hh.aiinterviewer.domain.model.MessageTrigger;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionStatus;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.llm.Prompts;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;
import ru.hh.aiinterviewer.utils.JsonUtils;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private static final int MAX_ITERATIONS = 100;
  private static final int DEFAULT_NUM_QUESTIONS = 5;

  private final VacancyService vacancyService;
  private final SessionRepository sessionRepository;
  private final ChatClient interviewerChatClient;
  private final ChatClient prepareInterviewPlanChatClient;

  @Transactional
  public CreateSessionResponse createSession(CreateSessionRequest request) {
    VacancyInfo vacancy = vacancyService.fetchVacancy(request.getVacancyUrl());
    int numQuestions = request.getNumQuestions() != null && request.getNumQuestions() > 0 ? request.getNumQuestions() : DEFAULT_NUM_QUESTIONS;

    String interviewPlan = prepareInterviewPlanChatClient
        .prompt()
        .user(Prompts.getPrepareInterviewPlanPrompt(JsonUtils.toJson(vacancy), numQuestions, request.getInstructions()))
        .call()
        .content();

    Session session = Session.builder()
        .vacancyUrl(vacancy.url())
        .vacancyTitle(vacancy.title())
        .status(SessionStatus.PLANNED)
        .numQuestions(numQuestions)
        .instructions(request.getInstructions())
        .build();

    session.addAssistantMessage(interviewPlan);

    sessionRepository.save(session);

    return buildCreateResponse(session, interviewPlan);
  }

  @Transactional
  public MessageResponse processMessage(UUID sessionId, String userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    session.ensureNotCompleted();

    if (session.getStatus() == SessionStatus.PLANNED) {
      return startInterview(session, userMessage);
    }

    if (session.getMessages().size() >= MAX_ITERATIONS) {
      return forceCompleteInterview(session, userMessage);
    }

    String assistantAnswer = performChatInteraction(session, userMessage);

    if (MessageTrigger.COMPLETE.isTrigger(assistantAnswer)) {
      return completedInterview(session, assistantAnswer);
    }

    return buildNextQuestionMessageResponse(session, assistantAnswer);
  }

  private MessageResponse startInterview(Session session, String userMessage) {
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

  private MessageResponse completedInterview(Session session, String feedback) {
    session.completeInterview(feedback);
    sessionRepository.save(session);
    return buildFeedbackMessageResponse(session, feedback);
  }

  private MessageResponse forceCompleteInterview(Session session, String userMessage) {
    String feedback = performChatInteraction(session, Prompts.getInterviewFinalFeedbackPrompt(userMessage));
    session.completeInterview(feedback);
    sessionRepository.save(session);
    return buildFeedbackMessageResponse(session, feedback);
  }

  private String performChatInteraction(Session session, String userMessage) {
    return interviewerChatClient.prompt()
        .user(userMessage)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, session.getId().toString()))
        .call()
        .content();
  }

  private MessageResponse buildNextQuestionMessageResponse(Session session, String question) {
    return MessageResponse.builder()
        .sessionId(session.getId().toString())
        .message(question)
        .interviewComplete(false)
        .build();
  }

  private MessageResponse buildFeedbackMessageResponse(Session session, String feedback) {
    return MessageResponse.builder()
        .sessionId(session.getId().toString())
        .message(feedback)
        .interviewComplete(true)
        .build();
  }

  public static CreateSessionResponse buildCreateResponse(Session session, String introMessage) {
    return CreateSessionResponse.builder()
        .sessionId(session.getId().toString())
        .introMessage(introMessage)
        .build();
  }
}
