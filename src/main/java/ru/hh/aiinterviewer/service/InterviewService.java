package ru.hh.aiinterviewer.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequest;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.MessageResponse;
import ru.hh.aiinterviewer.api.mapper.SessionMapper;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.domain.model.Message;
import ru.hh.aiinterviewer.domain.model.MessageRole;
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
    Message introMessage = Message.builder()
        .role(MessageRole.ASSISTANT)
        .content(intro)
        .build();
    session.addMessage(introMessage);

    sessionRepository.save(session);

    return SessionMapper.toCreateResponse(session.getId(), intro);
  }

  @Transactional
  public MessageResponse processMessage(UUID sessionId, String userMessage) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

    if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new IllegalStateException("Session is already completed");
    }

    if (session.getStatus() == SessionStatus.PLANNED) {
      String normalized = userMessage == null ? "" : userMessage.trim().toLowerCase();
      if (!(normalized.equals("начать интервью") || normalized.equals("start") || normalized.equals("start interview"))) {
        throw new IllegalArgumentException("To start interview, send 'Начать интервью'");
      }
      session.setStatus(SessionStatus.ONGOING);
      session.setStartedAt(OffsetDateTime.now());
      sessionRepository.save(session);

      String question = llmService.generateNextQuestion(buildHistory(session),
          session.getVacancyTitle() != null ? session.getVacancyTitle() : session.getVacancyUrl(),
          1, session.getNumQuestions(), session.getInstructions());

      Message assistantNewMessage = Message.builder()
          .role(MessageRole.ASSISTANT)
          .content(question)
          .build();

      session.addMessage(assistantNewMessage);

      sessionRepository.save(session);

      MessageResponse resp = new MessageResponse();
      resp.setSessionId(session.getId().toString());
      resp.setMessage(question);
      resp.setInterviewComplete(false);
      return resp;
    }

    // ongoing
    Message userNewMessage = Message.builder()
        .role(MessageRole.USER)
        .content(userMessage)
        .build();
    session.addMessage(userNewMessage);

    long userAnswers = session.getMessages().stream()
        .filter(m -> m.getRole() == MessageRole.USER).count();

    if (userAnswers >= session.getNumQuestions()) {
      String feedback = llmService.generateFinalFeedback(buildHistory(session),
          session.getVacancyTitle() != null ? session.getVacancyTitle() : session.getVacancyUrl(),
          session.getInstructions());

      Message assistantNewMessage = Message.builder()
          .role(MessageRole.ASSISTANT)
          .content(feedback)
          .build();
      session.addMessage(assistantNewMessage);
      session.setStatus(SessionStatus.COMPLETED);
      session.setEndedAt(OffsetDateTime.now());
      sessionRepository.save(session);

      MessageResponse resp = new MessageResponse();
      resp.setSessionId(session.getId().toString());
      resp.setMessage(feedback);
      resp.setInterviewComplete(true);
      return resp;
    } else {
      int nextIndex = (int) (userAnswers + 1);
      String question = llmService.generateNextQuestion(buildHistory(session),
          session.getVacancyTitle() != null ? session.getVacancyTitle() : session.getVacancyUrl(),
          nextIndex, session.getNumQuestions(), session.getInstructions());

      Message assistantNewMessage = Message.builder()
          .role(MessageRole.ASSISTANT)
          .content(question)
          .build();
      session.addMessage(assistantNewMessage);
      sessionRepository.save(session);

      MessageResponse resp = new MessageResponse();
      resp.setSessionId(session.getId().toString());
      resp.setMessage(question);
      resp.setInterviewComplete(false);
      return resp;
    }
  }

  private List<org.springframework.ai.chat.messages.Message> buildHistory(Session session) {
    List<org.springframework.ai.chat.messages.Message> history = new ArrayList<>(session.getMessages().size());
    for (Message me : session.getMessages()) {
      if (me.getRole() == MessageRole.ASSISTANT) {
        history.add(new AssistantMessage(me.getContent()));
      } else {
        history.add(new UserMessage(me.getContent()));
      }
    }
    return history;
  }
}


