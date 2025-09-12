package ru.hh.aiinterviewer.service;

import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.api.dto.SessionMessageDto;
import ru.hh.aiinterviewer.api.dto.SessionResponseDto;
import ru.hh.aiinterviewer.api.dto.SessionStatusResponseDto;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionMessage;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class InterviewQueryService {

  private final SessionRepository sessionRepository;

  public SessionStatusResponseDto getStatus(UUID sessionId) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    return SessionStatusResponseDto.builder()
        .status(session.getStatus().getValue())
        .build();
  }

  public SessionResponseDto getHistory(UUID sessionId) {
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
    return buildSessionResponse(session);
  }

  private SessionResponseDto buildSessionResponse(Session session) {
    return SessionResponseDto.builder()
        .sessionId(session.getId().toString())
        .status(session.getStatus().getValue())
        .mode(session.getMode())
        .vacancyUrl(session.getVacancyUrl())
        .roleName(session.getRoleName())
        .numQuestions(session.getNumQuestions())
        .interviewFormat(session.getInterviewFormat())
        .communicationStylePreset(session.getCommunicationStylePreset())
        .communicationStyleFreeform(session.getCommunicationStyleFreeform())
        .planPreferences(session.getPlanPreferences())
        .startedAt(session.getStartedAt())
        .endedAt(session.getEndedAt())
        .messages(session.getMessages().stream()
            .sorted(Comparator.comparing(SessionMessage::getCreatedAt))
            .map(m -> SessionMessageDto.builder()
                .role(m.getRole().getValue())
                .content(m.getContent())
                .build())
            .toList())
        .build();
  }
}
