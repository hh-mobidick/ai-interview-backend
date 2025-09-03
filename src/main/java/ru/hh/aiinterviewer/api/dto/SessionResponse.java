package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class SessionResponse {

  private String sessionId;
  private String vacancyTitle;
  private String vacancyUrl;
  private String status;
  private Integer numQuestions;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private String instructions;
  private List<SessionMessage> messages;

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getVacancyTitle() {
    return vacancyTitle;
  }

  public void setVacancyTitle(String vacancyTitle) {
    this.vacancyTitle = vacancyTitle;
  }

  public String getVacancyUrl() {
    return vacancyUrl;
  }

  public void setVacancyUrl(String vacancyUrl) {
    this.vacancyUrl = vacancyUrl;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getNumQuestions() {
    return numQuestions;
  }

  public void setNumQuestions(Integer numQuestions) {
    this.numQuestions = numQuestions;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public OffsetDateTime getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public List<SessionMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<SessionMessage> messages) {
    this.messages = messages;
  }

  public static class SessionMessage {
    private String role;
    private String content;

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }
}


