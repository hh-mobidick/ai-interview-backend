package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {

  PLANNED("planned"),
  ONGOING("ongoing"),
  COMPLETED("completed");

  @Getter
  private final String value;

  public static SessionStatus fromValue(String value) {
    return Arrays.stream(SessionStatus.values())
        .filter(status -> status.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown SessionStatus value: " + value));
  }
}
