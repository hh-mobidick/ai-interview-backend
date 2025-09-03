package ru.hh.aiinterviewer.api.mapper;

import java.util.UUID;
import java.util.stream.Collectors;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.SessionMessage;
import ru.hh.aiinterviewer.api.dto.SessionResponse;
import ru.hh.aiinterviewer.domain.model.Session;

public class SessionMapper {

  public static CreateSessionResponse toCreateResponse(UUID sessionId, String introMessage) {
    CreateSessionResponse resp = new CreateSessionResponse();
    resp.setSessionId(sessionId.toString());
    resp.setIntroMessage(introMessage);
    return resp;
  }

  public static SessionResponse toSessionResponse(Session session) {
    SessionResponse dto = new SessionResponse();
    dto.setSessionId(session.getId().toString());
    dto.setVacancyTitle(session.getVacancyTitle());
    dto.setVacancyUrl(session.getVacancyUrl());
    dto.setStatus(session.getStatus() == null ? null : session.getStatus().getValue());
    dto.setNumQuestions(session.getNumQuestions());
    dto.setStartedAt(session.getStartedAt());
    dto.setEndedAt(session.getEndedAt());
    dto.setInstructions(session.getInstructions());
    dto.setMessages(session.getMessages().stream().map(m -> {
      SessionMessage sm = new SessionMessage();
      sm.setRole(m.getRole() == null ? null : m.getRole().getValue());
      sm.setContent(m.getContent());
      return sm;
    }).collect(Collectors.toList()));
    return dto;
  }
}
