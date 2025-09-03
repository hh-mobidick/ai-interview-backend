package ru.hh.aiinterviewer.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.aiinterviewer.model.MessageEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

  List<MessageEntity> findAllBySession_IdOrderByCreatedAtAsc(UUID sessionId);
}


