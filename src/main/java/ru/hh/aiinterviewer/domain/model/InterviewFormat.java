package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum InterviewFormat {

  TRAINING("training"),
  REALISTIC("realistic");

  @Getter
  private final String value;

  public static InterviewFormat fromValue(String value) {
    return Arrays.stream(InterviewFormat.values())
        .filter(format -> format.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown InterviewFormat value: " + value));
  }
}


