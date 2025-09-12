package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageTrigger {

  PLAN("План интервью"),
  START("Начать интервью"),
  FEEDBACK("Обратная связь"),
  FINISH("Завершить интервью"),
  COMPLETE("Интервью завершено");

  private final String value;

  public static Optional<MessageTrigger> of(String message) {
    String normalizedMessage = normalize(message);
    return Arrays.stream(MessageTrigger.values())
        .filter(role -> role.getValue().toLowerCase().equals(normalizedMessage))
        .findFirst();
  }

  public boolean isTrigger(String message) {
    String normalizedMessage = normalize(message);
    return normalizedMessage != null && normalizedMessage.startsWith(value.toLowerCase());
  }

  private static String normalize(String message) {
    return Optional.ofNullable(message)
        .map(String::toLowerCase)
        .map(String::trim)
        .map(m -> m.replaceAll("<[^>]*>", ""))
        .orElse(null);
  }
}
