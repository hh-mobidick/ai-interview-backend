package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageRequestDto {

  @Pattern(regexp = "text|audio", message = "type must be 'text' or 'audio'")
  private String type;

  // Text message content (required when type=text)
  private String message;

  // Audio payload (base64-encoded) (required when type=audio). Only WAV supported.
  private String audioBase64;
}
