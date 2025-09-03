package ru.hh.aiinterviewer.api.dto;

import lombok.Data;

@Data
public class MessageResponse {

  private String sessionId;
  private String message;
  private boolean interviewComplete;
}
