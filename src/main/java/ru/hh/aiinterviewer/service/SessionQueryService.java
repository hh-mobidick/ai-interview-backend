package ru.hh.aiinterviewer.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.api.dto.SessionMessage;
import ru.hh.aiinterviewer.api.dto.SessionResponse;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class SessionQueryService {

  private final SessionRepository sessionRepository;

  public SessionResponse getSession(UUID sessionId) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    return buildSessionResponse(session);
  }

  private SessionResponse buildSessionResponse(Session session) {
    return SessionResponse.builder()
        .sessionId(session.getId().toString())
        .vacancyTitle(session.getVacancyTitle())
        .vacancyUrl(session.getVacancyUrl())
        .status(session.getStatus() == null ? null : session.getStatus().getValue())
        .numQuestions(session.getNumQuestions())
        .startedAt(session.getStartedAt())
        .endedAt(session.getEndedAt())
        .instructions(session.getInstructions())
        .messages(session.getMessages().stream()
            .map(m -> SessionMessage.builder()
                .role(m.getRole() == null ? null : m.getRole().getValue())
                .content(m.getContent())
                .build())
            .toList())
        .build();
  }
}
