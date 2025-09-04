package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {

  private String sessionId;
  private String vacancyUrl;
  private String status;//TODO enum
  private Integer numQuestions;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private String instructions;
  private List<SessionMessage> messages;
}
