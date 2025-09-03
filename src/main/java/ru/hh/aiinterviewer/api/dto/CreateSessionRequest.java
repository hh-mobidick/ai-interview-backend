package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateSessionRequest {

  @NotBlank
  private String vacancyUrl;

  @Min(1)
  private Integer numQuestions;

  private String instructions;

  public String getVacancyUrl() {
    return vacancyUrl;
  }

  public void setVacancyUrl(String vacancyUrl) {
    this.vacancyUrl = vacancyUrl;
  }

  public Integer getNumQuestions() {
    return numQuestions;
  }

  public void setNumQuestions(Integer numQuestions) {
    this.numQuestions = numQuestions;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }
}


