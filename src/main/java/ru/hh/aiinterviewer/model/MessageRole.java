package ru.hh.aiinterviewer.model;

public enum MessageRole {
  ASSISTANT,
  USER;

  public static MessageRole fromDatabaseValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("MessageRole value is null");
    }
    switch (value.toLowerCase()) {
      case "assistant":
        return ASSISTANT;
      case "user":
        return USER;
      default:
        throw new IllegalArgumentException("Unknown MessageRole value: " + value);
    }
  }

  public String toDatabaseValue() {
    switch (this) {
      case ASSISTANT:
        return "assistant";
      case USER:
        return "user";
      default:
        throw new IllegalStateException("Unexpected MessageRole: " + this);
    }
  }
}


