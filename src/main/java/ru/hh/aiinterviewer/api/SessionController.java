package ru.hh.aiinterviewer.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequest;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponse;
import ru.hh.aiinterviewer.api.dto.MessageRequest;
import ru.hh.aiinterviewer.api.dto.MessageResponse;
import ru.hh.aiinterviewer.api.dto.SessionResponse;
import ru.hh.aiinterviewer.service.InterviewService;
import ru.hh.aiinterviewer.service.SessionQueryService;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final InterviewService interviewService;
  private final SessionQueryService sessionQueryService;

  @PostMapping
  public ResponseEntity<CreateSessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
    CreateSessionResponse response = interviewService.createSession(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{sessionId}/messages")
  public ResponseEntity<MessageResponse> addMessage(
      @PathVariable("sessionId") String sessionId,
      @Valid @RequestBody MessageRequest request
  ) {
    UUID id = UUID.fromString(sessionId);
    MessageResponse response = interviewService.processMessage(id, request.getMessage());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{sessionId}")
  public ResponseEntity<SessionResponse> get(@PathVariable("sessionId") String sessionId) {
    UUID id = UUID.fromString(sessionId);
    SessionResponse response = sessionQueryService.getSession(id);
    return ResponseEntity.ok(response);
  }
}
