package ru.hh.aiinterviewer.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSessionResponseDto {

  private String sessionId;
  private String introMessage;
}
