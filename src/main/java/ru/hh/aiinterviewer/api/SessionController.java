package ru.hh.aiinterviewer.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageResponseDto;
import ru.hh.aiinterviewer.api.dto.SessionResponseDto;
import ru.hh.aiinterviewer.api.dto.SessionStatusResponseDto;
import ru.hh.aiinterviewer.service.InterviewQueryService;
import ru.hh.aiinterviewer.service.InterviewService;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final InterviewService interviewService;
  private final InterviewQueryService interviewQueryService;

  @PostMapping
  public ResponseEntity<SessionResponseDto> create(@Valid @RequestBody CreateSessionRequestDto request) {
    UUID sessionId = interviewService.createSession(request);
    SessionResponseDto response = interviewQueryService.getHistory(sessionId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{sessionId}/messages")
  public ResponseEntity<MessageResponseDto> addMessage(
      @PathVariable("sessionId") String sessionId,
      @Valid @RequestBody MessageRequestDto request
  ) {
    UUID id = UUID.fromString(sessionId);
    MessageResponseDto response = interviewService.processMessage(id, request.getMessage());
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter addMessageStream(
      @PathVariable("sessionId") String sessionId,
      @Valid @RequestBody MessageRequestDto request
  ) {
    UUID id = UUID.fromString(sessionId);
    return interviewService.processMessageStream(id, request.getMessage());
  }

  @GetMapping("/{sessionId}")
  public ResponseEntity<SessionResponseDto> getSession(@PathVariable("sessionId") String sessionId) {
    UUID id = UUID.fromString(sessionId);
    SessionResponseDto response = interviewQueryService.getHistory(id);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{sessionId}/status")
  public ResponseEntity<SessionStatusResponseDto> getSessionStatus(@PathVariable("sessionId") String sessionId) {
    UUID id = UUID.fromString(sessionId);
    SessionStatusResponseDto response = interviewQueryService.getStatus(id);
    return ResponseEntity.ok(response);
  }
}
