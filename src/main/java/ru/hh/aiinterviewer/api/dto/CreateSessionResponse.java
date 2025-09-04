package ru.hh.aiinterviewer.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSessionResponse {

  private String sessionId;
  private String introMessage;
}
