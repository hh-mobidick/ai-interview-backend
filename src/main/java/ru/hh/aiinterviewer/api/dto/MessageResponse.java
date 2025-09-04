package ru.hh.aiinterviewer.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {

  private String sessionId;
  private String message;
  private boolean interviewComplete;
}
