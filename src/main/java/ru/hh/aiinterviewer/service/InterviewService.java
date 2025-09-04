package ru.hh.aiinterviewer.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequest;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.MessageResponse;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionStatus;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private static final int DEFAULT_NUM_QUESTIONS = 5;

  private final VacancyService vacancyService;
  private final LLMService llmService;
  private final SessionRepository sessionRepository;

  @Transactional
  public CreateSessionResponse createSession(CreateSessionRequest request) {
    VacancyInfo vacancy = vacancyService.fetchVacancy(request.getVacancyUrl());
    int numQuestions = request.getNumQuestions() != null && request.getNumQuestions() > 0 ? request.getNumQuestions() : DEFAULT_NUM_QUESTIONS;

    String intro = llmService.prepareInterviewPlan(
        vacancy,
        numQuestions,
        request.getInstructions()
    );

    Session session = Session.builder()
        .vacancyUrl(vacancy.url())
        .vacancyTitle(vacancy.title())
        .status(SessionStatus.PLANNED)
        .numQuestions(numQuestions)
        .instructions(request.getInstructions())
        .build();
    session.addAssistantMessage(intro);
    sessionRepository.save(session);

    return buildCreateResponse(session, intro);
  }

  private MessageResponse startInterview(Session session, String userMessage) {
    session.startInterview(userMessage);
    sessionRepository.save(session);

    String question = llmService.generateNextQuestion(
        session.toChatHistory(),
        session.getVacancyTitleOrUrl(),
        1,
        session.getNumQuestions(),
        session.getInstructions()
    );
    session.addAssistantMessage(question);
    sessionRepository.save(session);

    return buildNextQuestionMessageResponse(session, question);
  }

  private MessageResponse completeInterview(Session session, String userMessage) {
    String feedback = llmService.generateFinalFeedback(
        session.toChatHistory(),
        session.getVacancyTitleOrUrl(),
        session.getInstructions()
    );
    session.completeInterview(feedback);
    sessionRepository.save(session);

    return buildFeedbackMessageResponse(session, feedback);
  }

  @Transactional
  public MessageResponse processMessage(UUID sessionId, String userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    session.ensureNotCompleted();

    if (session.getStatus() == SessionStatus.PLANNED) {
      return startInterview(session, userMessage);
    }

    session.addUserMessage(userMessage);

    long userAnswers = session.getUserAnswersCount();

    if (userAnswers >= session.getNumQuestions()) {
      return completeInterview(session, userMessage);
    }

    int nextIndex = session.getNextQuestionIndex();
    String question = llmService.generateNextQuestion(
        session.toChatHistory(),
        session.getVacancyTitleOrUrl(),
        nextIndex,
        session.getNumQuestions(),
        session.getInstructions()
    );

    session.addAssistantMessage(question);
    sessionRepository.save(session);

    return buildNextQuestionMessageResponse(session, question);
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


