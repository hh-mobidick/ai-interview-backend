package ru.hh.aiinterviewer.model;

public enum SessionStatus {
  PLANNED,
  ONGOING,
  COMPLETED;

  public static SessionStatus fromDatabaseValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("SessionStatus value is null");
    }
    switch (value.toLowerCase()) {
      case "planned":
        return PLANNED;
      case "ongoing":
        return ONGOING;
      case "completed":
        return COMPLETED;
      default:
        throw new IllegalArgumentException("Unknown SessionStatus value: " + value);
    }
  }

  public String toDatabaseValue() {
    switch (this) {
      case PLANNED:
        return "planned";
      case ONGOING:
        return "ongoing";
      case COMPLETED:
        return "completed";
      default:
        throw new IllegalStateException("Unexpected SessionStatus: " + this);
    }
  }
}


