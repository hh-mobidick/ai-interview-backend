package ru.hh.aiinterviewer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.llm.Prompts;
import ru.hh.aiinterviewer.llm.SessionChatMemory;

@Configuration
public class ApplicationConfig {

  @Autowired
  private SessionRepository sessionRepository;

  @Bean
  public ChatClient interviwerChatClient(ChatClient.Builder builder) {
    return builder.defaultAdvisors(
            getSessionHistoryAdvisor(0),
            SimpleLoggerAdvisor.builder().order(1).build()
        )
        .defaultSystem(Prompts.getInterviewerPrompt())
        .build();
  }

  private Advisor getSessionHistoryAdvisor(int order) {
    return MessageChatMemoryAdvisor.builder(getSessionChatMemory()).order(order).build();
  }

  private ChatMemory getSessionChatMemory() {
    return SessionChatMemory.builder()
        .maxMessages(8)
        .sessionRepository(sessionRepository)
        .build();
  }
}
