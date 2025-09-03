package ru.hh.aiinterviewer.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private UUID id;

  @Column(name = "vacancy_url", nullable = false)
  private String vacancyUrl;

  @Column(name = "vacancy_title")
  private String vacancyTitle;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SessionStatus status;

  @Column(name = "num_questions", nullable = false)
  private Integer numQuestions;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "ended_at")
  private OffsetDateTime endedAt;

  @Column(name = "instructions")
  private String instructions;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @OrderBy("createdAt ASC")
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "session_id")
  private List<Message> messages = new ArrayList<>();

  public void addMessage(Message message) {
    messages.add(message);
  }
}
