package ru.hh.aiinterviewer.llm;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.ai.chat.prompt.PromptTemplate;

@UtilityClass
public class Prompts {

  private static final PromptTemplate PREPARE_INTERVIEW_PLAN_PROMPT = new PromptTemplate("""
      """);//TODO

  private static final PromptTemplate INTERVIEWER_SYSTEM_PROMPT = new PromptTemplate("""
      """);//TODO

  private static final PromptTemplate FORCE_INTERVIEW_FEEDBACK_PROMPT = new PromptTemplate("""
      """);//TODO

  public static String getPrepareInterviewPlanPrompt(
      String vacancy,
      int questionNumber,
      String uservInstructions
  ) {
    return PREPARE_INTERVIEW_PLAN_PROMPT.render(Map.of(
        "VACANCY", vacancy,
        "QUESTION_NUMBER", questionNumber,
        "USER_INSTRUCTIONS", uservInstructions
    ));
  }

  public static String getInterviewerPrompt() {
    return INTERVIEWER_SYSTEM_PROMPT.render();
  }

  public static String getInterviewFinalFeedbackPrompt(String userMessage) {
    return FORCE_INTERVIEW_FEEDBACK_PROMPT.render(Map.of("USER_MESSAGE", userMessage));
  }
}
