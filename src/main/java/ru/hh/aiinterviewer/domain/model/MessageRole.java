package ru.hh.aiinterviewer.domain.model;

import java.util.Arrays;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

@RequiredArgsConstructor
public enum MessageRole {

  USER("user", UserMessage::new),
  ASSISTANT("assistant", AssistantMessage::new),
  SYSTEM("system", SystemMessage::new);

  @Getter
  private final String value;
  private final Function<String, Message> toMessageFunction;

  public static MessageRole fromValue(String value) {
    return Arrays.stream(MessageRole.values())
        .filter(role -> role.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown MessageRole value: " + value));
  }

  public Message getMessage(String message) {
    return toMessageFunction.apply(message);
  }
}
