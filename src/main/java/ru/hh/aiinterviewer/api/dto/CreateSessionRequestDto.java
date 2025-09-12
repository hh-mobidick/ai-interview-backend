package ru.hh.aiinterviewer.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import ru.hh.aiinterviewer.domain.model.InterviewFormat;
import ru.hh.aiinterviewer.domain.model.SessionMode;

@Data
@Builder
public class CreateSessionRequestDto {

  @NotNull
  private SessionMode mode;

  // Vacancy sources (exactly one required on backend for mode=vacancy)
  private String vacancyUrl;
  private String vacancyText;

  // For mode=role
  private String roleName;

  @Min(1)
  @Max(50)
  private Integer numQuestions;

  private String planPreferences;

  private InterviewFormat interviewFormat;

  private String communicationStylePreset;
  private String communicationStyleFreeform;
}


