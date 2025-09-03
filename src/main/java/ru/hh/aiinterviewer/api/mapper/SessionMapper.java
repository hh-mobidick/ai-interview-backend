package ru.hh.aiinterviewer.api.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.SessionResponse;
import ru.hh.aiinterviewer.model.MessageEntity;
import ru.hh.aiinterviewer.model.SessionEntity;

public class SessionMapper {

  public static CreateSessionResponse toCreateResponse(UUID sessionId, String introMessage) {
    CreateSessionResponse resp = new CreateSessionResponse();
    resp.setSessionId(sessionId.toString());
    resp.setIntroMessage(introMessage);
    return resp;
  }

  public static SessionResponse toSessionResponse(SessionEntity session, List<MessageEntity> messages) {
    SessionResponse dto = new SessionResponse();
    dto.setSessionId(session.getId().toString());
    dto.setVacancyTitle(session.getVacancyTitle());
    dto.setVacancyUrl(session.getVacancyUrl());
    dto.setStatus(session.getStatus() == null ? null : session.getStatus().toDatabaseValue());
    dto.setNumQuestions(session.getNumQuestions());
    dto.setStartedAt(session.getStartedAt());
    dto.setEndedAt(session.getEndedAt());
    dto.setInstructions(session.getInstructions());
    dto.setMessages(messages.stream().map(m -> {
      SessionResponse.SessionMessage sm = new SessionResponse.SessionMessage();
      sm.setRole(m.getRole() == null ? null : m.getRole().toDatabaseValue());
      sm.setContent(m.getContent());
      return sm;
    }).collect(Collectors.toList()));
    return dto;
  }
}


