package ru.hh.aiinterviewer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "roles")
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "normalized", nullable = false)
  private String normalized;

  @Column(name = "popularity", nullable = false)
  private Integer popularity;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
