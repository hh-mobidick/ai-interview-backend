package ru.hh.aiinterviewer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", nullable = false)
  private SessionEntity session;

  @Column(name = "role", nullable = false)
  private MessageRole role;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public SessionEntity getSession() {
    return session;
  }

  public void setSession(SessionEntity session) {
    this.session = session;
  }

  public MessageRole getRole() {
    return role;
  }

  public void setRole(MessageRole role) {
    this.role = role;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}


