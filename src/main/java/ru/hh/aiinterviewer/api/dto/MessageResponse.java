package ru.hh.aiinterviewer.api.dto;

public class MessageResponse {

  private String sessionId;
  private String message;
  private boolean interviewComplete;

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isInterviewComplete() {
    return interviewComplete;
  }

  public void setInterviewComplete(boolean interviewComplete) {
    this.interviewComplete = interviewComplete;
  }
}


