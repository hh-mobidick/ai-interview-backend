package ru.hh.aiinterviewer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "role_synonyms")
@NoArgsConstructor
@AllArgsConstructor
public class RoleSynonym {

  @Id
  @Builder.Default
  private UUID id = UUID.randomUUID();

  @ManyToOne(optional = false)
  @JoinColumn(name = "role_id")
  private Role role;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "normalized", nullable = false)
  private String normalized;
}
