package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageTrigger {

  START("начать интервью");

  private final String message;

  public static Optional<MessageTrigger> of(String message) {
    String normalizedMessage = Optional.ofNullable(message).map(String::toLowerCase).map(String::trim).orElse(null);
    return Arrays.stream(MessageTrigger.values())
        .filter(role -> role.getMessage().equals(normalizedMessage))
        .findFirst();
  }
}
