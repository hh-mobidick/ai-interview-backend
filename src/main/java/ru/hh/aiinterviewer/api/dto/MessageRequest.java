package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.NotBlank;

public class MessageRequest {

  @NotBlank
  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}


