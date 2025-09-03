package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionRequest {

  @NotBlank
  private String vacancyUrl;
  @Min(1)
  private Integer numQuestions;
  private String instructions;
}


