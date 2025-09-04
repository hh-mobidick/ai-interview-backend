package ru.hh.aiinterviewer.llm;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionMessage;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;

@Builder
public class SessionChatMemory implements ChatMemory {

    private SessionRepository sessionRepository;

    private int maxMessages;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Session session = sessionRepository.findById(UUID.fromString(conversationId)).orElseThrow();
        for (Message message : messages) {
            session.addMessage(SessionMessage.from(message));
        }
        sessionRepository.save(session);
    }


    @Override
    public List<Message> get(String conversationId) {
        Session session = sessionRepository.findById(UUID.fromString(conversationId)).orElseThrow();
        return session.getMessages().stream()
                .map(SessionMessage::toMessage)
                .toList();

    }

    @Override
    public void clear(String conversationId) {
        throw new UnsupportedOperationException();
    }
}
