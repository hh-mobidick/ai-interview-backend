package ru.hh.aiinterviewer.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity
@Builder
@Table(name = "sessions")
@NoArgsConstructor
@AllArgsConstructor
public class Session {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @Column(name = "vacancy_url")
  private String vacancyUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private SessionStatus status;

  @Column(name = "num_questions")
  private Integer numQuestions;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "ended_at")
  private OffsetDateTime endedAt;

  @Column(name = "instructions")
  private String instructions;

  @CreationTimestamp
  @Column(name = "created_at")
  private OffsetDateTime createdAt;

  @Builder.Default
  @OrderBy("createdAt ASC")
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL,orphanRemoval = true)
  @JoinColumn(name = "session_id")
  private List<SessionMessage> messages = new ArrayList<>();

  public void addMessage(SessionMessage message) {
    messages.add(message);
  }

  public boolean isPlanned() {
    return status == SessionStatus.PLANNED;
  }

  public boolean isCompleted() {
    return status == SessionStatus.COMPLETED;
  }

  public void ensureNotCompleted() {
    if (isCompleted()) {
      throw new IllegalStateException("Session is already completed");
    }
  }

  public void startInterview() {
    if (!isPlanned()) {
      throw new IllegalStateException("Session is not in planned status");
    }
    status = SessionStatus.ONGOING;
    startedAt = OffsetDateTime.now();
  }

  public void completeInterview() {
    status = SessionStatus.COMPLETED;
    endedAt = OffsetDateTime.now();
  }
}
