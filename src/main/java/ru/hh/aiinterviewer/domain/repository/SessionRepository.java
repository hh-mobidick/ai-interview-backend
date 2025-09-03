package ru.hh.aiinterviewer.domain.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.aiinterviewer.domain.model.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {
}
