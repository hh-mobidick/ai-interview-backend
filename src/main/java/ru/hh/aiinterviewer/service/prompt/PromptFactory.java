package ru.hh.aiinterviewer.service.prompt;

import java.util.List;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

  public String buildSystemPrompt(String extraInstructions) {
    StringBuilder sb = new StringBuilder();
    sb.append("Ты — строгий и доброжелательный интервьюер, проводящий собеседование по вакансии. ");
    sb.append("Говори по-русски, будь кратким и структурированным. ");
    sb.append("Порядок: 1) Вступление (summary вакансии + план тем), 2) Q&A по одной теме за раз, 3) Финальный фидбек. ");
    sb.append("Финальный фидбек должен содержать: общая оценка (1–5), соответствие вакансии, сильные стороны, слабые стороны, оценка по темам, рекомендации. ");
    if (extraInstructions != null && !extraInstructions.isBlank()) {
      sb.append("Дополнительные инструкции: ").append(extraInstructions.trim()).append(" ");
    }
    return sb.toString();
  }

  public String introUserPrompt(String vacancyTitle, String vacancySummary, int numQuestions) {
    StringBuilder sb = new StringBuilder();
    sb.append("Суммаризируй вакансию и предложи план интервью на ").append(numQuestions).append(" вопросов.\\n");
    sb.append("Название: ").append(vacancyTitle == null ? "" : vacancyTitle).append("\\n");
    if (vacancySummary != null && !vacancySummary.isBlank()) {
      sb.append("Описание: ").append(truncate(vacancySummary, 1800)).append("\\n");
    }
    sb.append("В конце попроси пользователя отправить фразу 'Начать интервью'.");
    return sb.toString();
  }

  public String nextQuestionUserPrompt(int questionIndex, int numQuestions, List<String> plannedTopics) {
    StringBuilder sb = new StringBuilder();
    sb.append("Продолжай интервью. Задай вопрос ").append(questionIndex).append("/").append(numQuestions).append(". ");
    sb.append("Один вопрос за раз. ");
    if (plannedTopics != null && !plannedTopics.isEmpty()) {
      StringJoiner joiner = new StringJoiner(", ");
      plannedTopics.forEach(joiner::add);
      sb.append("Фокус текущего вопроса: ").append(joiner.toString()).append(". ");
    }
    sb.append("Если ответ краткий — можно задать короткий фоллоуап.");
    return sb.toString();
  }

  public String finalFeedbackUserPrompt() {
    return "Заверши интервью и сформируй финальный фидбек с секциями: 1) Общая оценка (1–5) с кратким обоснованием; 2) Соответствие вакансии; 3) Сильные стороны; 4) Слабые стороны; 5) Оценка по темам; 6) Рекомендации по улучшению. Оформи в структурированном виде.";
  }

  private String truncate(String text, int max) {
    if (text == null) return null;
    return text.length() <= max ? text : text.substring(0, max);
  }
}


