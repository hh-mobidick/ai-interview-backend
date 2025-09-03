package ru.hh.aiinterviewer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

  @Id
  private UUID id;

  @Column(name = "vacancy_url", nullable = false)
  private String vacancyUrl;

  @Column(name = "vacancy_title")
  private String vacancyTitle;

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

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getVacancyUrl() {
    return vacancyUrl;
  }

  public void setVacancyUrl(String vacancyUrl) {
    this.vacancyUrl = vacancyUrl;
  }

  public String getVacancyTitle() {
    return vacancyTitle;
  }

  public void setVacancyTitle(String vacancyTitle) {
    this.vacancyTitle = vacancyTitle;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public void setStatus(SessionStatus status) {
    this.status = status;
  }

  public Integer getNumQuestions() {
    return numQuestions;
  }

  public void setNumQuestions(Integer numQuestions) {
    this.numQuestions = numQuestions;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public OffsetDateTime getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public String getInstructions() {
    return instructions;
  }

  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}


