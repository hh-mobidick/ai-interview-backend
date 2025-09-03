package ru.hh.aiinterviewer.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.api.dto.SessionResponse;
import ru.hh.aiinterviewer.api.mapper.SessionMapper;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;

@Service
@RequiredArgsConstructor
public class SessionQueryService {

  private final SessionRepository sessionRepository;

  public SessionResponse getSession(UUID sessionId) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    return SessionMapper.toSessionResponse(session);
  }
}
