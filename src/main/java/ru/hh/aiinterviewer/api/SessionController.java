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
import ru.hh.aiinterviewer.api.dto.CreateSessionRequestDto;
import ru.hh.aiinterviewer.api.dto.CreateSessionResponseDto;
import ru.hh.aiinterviewer.api.dto.MessageRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageResponseDto;
import ru.hh.aiinterviewer.api.dto.SessionResponseDto;
import ru.hh.aiinterviewer.service.InterviewService;
import ru.hh.aiinterviewer.service.InterviewQueryService;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final InterviewService interviewService;
  private final InterviewQueryService sessionQueryService;

  @PostMapping
  public ResponseEntity<CreateSessionResponseDto> create(@Valid @RequestBody CreateSessionRequestDto request) {
    CreateSessionResponseDto response = interviewService.createSession(request);
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

  @GetMapping("/{sessionId}")
  public ResponseEntity<SessionResponseDto> get(@PathVariable("sessionId") String sessionId) {
    UUID id = UUID.fromString(sessionId);
    SessionResponseDto response = sessionQueryService.getHistory(id);
    return ResponseEntity.ok(response);
  }
}
