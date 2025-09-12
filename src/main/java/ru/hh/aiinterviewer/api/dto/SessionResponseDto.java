package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import ru.hh.aiinterviewer.domain.model.InterviewFormat;
import ru.hh.aiinterviewer.domain.model.SessionMode;

@Data
@Builder
public class SessionResponseDto {

  private String sessionId;
  private String status;
  private SessionMode mode;
  private String vacancyUrl;
  private String roleName;
  private Integer numQuestions;
  private InterviewFormat interviewFormat;
  private String communicationStylePreset;
  private String communicationStyleFreeform;
  private String planPreferences;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private List<SessionMessageDto> messages;
}
