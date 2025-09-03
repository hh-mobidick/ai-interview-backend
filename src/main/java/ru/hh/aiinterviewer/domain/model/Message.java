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

@Data
@Entity
@Builder
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private MessageRole role;

  @Column(name = "content", nullable = false)
  private String content;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
