package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageTrigger {

  START("начать интервью"),
  COMPLETE("интервью завершено");

  private final String value;

  public static Optional<MessageTrigger> of(String message) {
    String normalizedMessage = Optional.ofNullable(message).map(String::toLowerCase).map(String::trim).orElse(null);
    return Arrays.stream(MessageTrigger.values())
        .filter(role -> role.getValue().equals(normalizedMessage))
        .findFirst();
  }

  public boolean isTrigger(String message) {
    String normalizedMessage = Optional.ofNullable(message).map(String::toLowerCase).map(String::trim).orElse(null);
    return normalizedMessage != null && normalizedMessage.startsWith(value);
  }
}
