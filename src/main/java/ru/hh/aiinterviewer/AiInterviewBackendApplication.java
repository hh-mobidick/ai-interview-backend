package ru.hh.aiinterviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import ru.hh.aiinterviewer.service.VacancyService;

@SpringBootApplication
public class AiInterviewBackendApplication {

  public static void main(String[] args) {
    ApplicationContext applicationContext = SpringApplication.run(AiInterviewBackendApplication.class, args);

    VacancyService vacancyService = applicationContext.getBean(VacancyService.class);
    System.out.println();
  }

}
