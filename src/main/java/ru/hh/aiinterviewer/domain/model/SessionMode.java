package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SessionMode {

  VACANCY("vacancy"),
  ROLE("role");

  @Getter
  private final String value;

  public static SessionMode fromValue(String value) {
    return Arrays.stream(SessionMode.values())
        .filter(mode -> mode.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown SessionMode value: " + value));
  }
}


