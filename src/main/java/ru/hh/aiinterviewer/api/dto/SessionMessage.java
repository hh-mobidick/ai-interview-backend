package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionMessage {

  @NotBlank//TODO здесь enum
  private String role;
  @NotBlank
  private String content;
}
