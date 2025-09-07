package ru.hh.aiinterviewer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.ai.chat.messages.Message;

@Data
@Entity
@Builder
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  private MessageRole role;

  private boolean internal;

  @Column(name = "content")
  private String content;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  public static SessionMessage newAssistantMessage(String message) {
    return SessionMessage.builder()
        .role(MessageRole.ASSISTANT)
        .content(message)
        .build();
  }

  public static SessionMessage from(Message message) {
    return SessionMessage.builder()
        .role(MessageRole.fromValue(message.getMessageType().getValue()))
        .content(message.getText())
        .build();
  }

  public Message toMessage() {
    return role.getMessage(content);
  }
}
