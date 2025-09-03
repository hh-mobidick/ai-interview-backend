package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageRole {

  ASSISTANT("assistant"),
  USER("user"),
  SYSTEM("system");

  @Getter
  private final String value;

  public static MessageRole fromValue(String value) {
    return Arrays.stream(MessageRole.values())
        .filter(role -> role.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown MessageRole value: " + value));
  }
}
