package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponseDto {

  private String sessionId;
  private String vacancyUrl;
  private String status;
  private Integer numQuestions;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private String instructions;
  private List<SessionMessageDto> messages;
}
