package ru.hh.aiinterviewer.llm;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.ai.chat.prompt.PromptTemplate;

@UtilityClass
public class Prompts {

  private static final PromptTemplate PARSE_VACANCY_PROMPT = new PromptTemplate("""
      Ты - эксперт по анализу вакансий и подбору персонала. ...
      ФОРМАТ ОТВЕТА (строго JSON):
      {{
        "position": "Название должности",
        "level": "junior/middle/senior/не указан",
        "key_responsibilities": [
          "Обязанность 1",
          "Обязанность 2"
        ],
        "required_skills": {{
          "technical": [
            "Навык 1",
            "Навык 2"
          ],
          "technologies": [
            "Технология 1",
            "Технология 2"
          ],
          "experience": "Требуемый опыт работы"
        }},
        "optional_skills": [
          "Желательный навык 1",
          "Желательный навык 2"
        ],
        "soft_skills": [
          "Soft skill 1",
          "Soft skill 2"
        ],
        "domain_knowledge": [
          "Знание предметной области 1",
          "Знание предметной области 2"
        ],
        "interview_focus_areas": [
          "Ключевая область для проверки 1",
          "Ключевая область для проверки 2"
        ],
        "company_specific": {{
          "industry": "Отрасль компании",
          "product": "Продукт/сервис компании",
          "team_size": "Размер команды",
          "methodology": "Методология работы (Agile/Scrum/etc)"
        }}
      }}
      
      Проанализируй предоставленную вакансию и верни результат в указанном JSON формате.
      
      {vacancy}
      """);

  private static final PromptTemplate PREPARE_INTERVIEW_PLAN_PROMPT = new PromptTemplate("""
      TODO
      """);//TODO

  private static final PromptTemplate INTERVIEWER_SYSTEM_PROMPT = new PromptTemplate("""
      TODO
      """);//TODO

  private static final PromptTemplate FORCE_INTERVIEW_FEEDBACK_PROMPT = new PromptTemplate("""
      TODO
      """);//TODO

  public static String getParseVacancyPrompt(String vacancy) {
    return PARSE_VACANCY_PROMPT.render(Map.of("vacancy", vacancy));
  }

  public static String getPrepareInterviewPlanPrompt(
      String vacancy,
      int questionNumber,
      String uservInstructions
  ) {
    return PREPARE_INTERVIEW_PLAN_PROMPT.render(Map.of(
        "vacancy", vacancy,
        "question_number", questionNumber,
        "user_instructions", uservInstructions
    ));
  }

  public static String getInterviewerPrompt() {
    return INTERVIEWER_SYSTEM_PROMPT.render();
  }

  public static String getInterviewFinalFeedbackPrompt(String userMessage) {
    return FORCE_INTERVIEW_FEEDBACK_PROMPT.render(Map.of("user_message", userMessage));
  }
}
