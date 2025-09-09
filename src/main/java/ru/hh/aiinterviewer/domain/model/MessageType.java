package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MessageType {

  TEXT("text"),
  AUDIO("audio");

  @Getter
  private final String value;

  public static MessageType fromValue(String value) {
    return Arrays.stream(MessageType.values())
        .filter(role -> role.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown MessageType value: " + value));
  }
}
