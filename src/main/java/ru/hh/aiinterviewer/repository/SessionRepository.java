package ru.hh.aiinterviewer.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.aiinterviewer.model.SessionEntity;

public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
}


