package ru.hh.aiinterviewer.service;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;
import ru.hh.aiinterviewer.service.prompt.PromptFactory;

@Service
public class LLMService {

  private static final Logger log = LoggerFactory.getLogger(LLMService.class);

  private final ChatClient chatClient;
  private final PromptFactory promptFactory;

  public LLMService(ChatClient.Builder chatClientBuilder, PromptFactory promptFactory) {
    this.chatClient = chatClientBuilder.build();
    this.promptFactory = promptFactory;
  }

  public String prepareInterviewPlan(VacancyInfo vacancyInfo, int numQuestions, String instructions) {
    String sys = promptFactory.buildSystemPrompt(instructions);
    String user = promptFactory.introUserPrompt(vacancyInfo.title(), vacancyInfo.description(), numQuestions);
    return doChat(sys, user);
  }

  public String generateNextQuestion(List<Message> history, String vacancyTitle, int questionIndex, int numQuestions, String instructions) {
    String sys = promptFactory.buildSystemPrompt(instructions);
    String user = promptFactory.nextQuestionUserPrompt(questionIndex, numQuestions, java.util.List.of());
    return doChat(sys, user, history);
  }

  public String generateFinalFeedback(List<Message> history, String vacancyTitle, String instructions) {
    String sys = promptFactory.buildSystemPrompt(instructions);
    String user = promptFactory.finalFeedbackUserPrompt();
    return doChat(sys, user, history);
  }

  private String doChat(String system, String user) {
    return doChat(system, user, List.of());
  }

  private String doChat(String system, String user, List<Message> history) {
    try {
      // Build a combined message list: system + history + user
      List<Message> messages = new java.util.ArrayList<>();
      messages.add(new SystemMessage(system));
      if (history != null && !history.isEmpty()) {
        messages.addAll(history);
      }
      messages.add(new UserMessage(user));

      String content = chatClient
          .prompt()
          .messages(messages)
          .call()
          .content();
      return content == null ? "" : content;
    } catch (Exception e) {
      log.error("LLM call failed: {}", e.getMessage());
      throw e;
    }
  }
}


