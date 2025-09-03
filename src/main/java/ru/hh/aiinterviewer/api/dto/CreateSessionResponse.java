package ru.hh.aiinterviewer.api.dto;

public class CreateSessionResponse {

  private String sessionId;
  private String introMessage;

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getIntroMessage() {
    return introMessage;
  }

  public void setIntroMessage(String introMessage) {
    this.introMessage = introMessage;
  }
}


