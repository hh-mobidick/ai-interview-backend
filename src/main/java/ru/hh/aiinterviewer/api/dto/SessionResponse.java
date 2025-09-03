package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SessionResponse {

  private String sessionId;
  private String vacancyTitle;
  private String vacancyUrl;
  private String status;//TODO enum
  private Integer numQuestions;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private String instructions;
  private List<SessionMessage> messages;
}
