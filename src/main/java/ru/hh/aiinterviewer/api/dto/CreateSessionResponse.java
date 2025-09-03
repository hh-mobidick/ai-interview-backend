package ru.hh.aiinterviewer.api.dto;

import lombok.Data;

@Data
public class CreateSessionResponse {

  private String sessionId;
  private String introMessage;
}
