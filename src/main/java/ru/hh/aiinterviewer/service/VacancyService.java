package ru.hh.aiinterviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;

import ru.hh.aiinterviewer.llm.Prompts;
import ru.hh.aiinterviewer.exception.NotFoundException;

@Service
public class VacancyService {

    private static final String VACANCY_OUTPUT_SCHEMA = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "title": "Результат анализа вакансии",
                  "type": "object",
                  "additionalProperties": false,
                  "required": [
                    "position",
                    "level",
                    "key_responsibilities",
                    "required_skills",
                    "optional_skills",
                    "soft_skills",
                    "domain_knowledge",
                    "interview_focus_areas",
                    "company_specific"
                  ],
                  "properties": {
                    "position": {
                      "type": "string",
                      "description": "Название должности, как указано в вакансии"
                    },
                    "level": {
                      "type": "string",
                      "description": "Уровень позиции, если явно указан",
                      "enum": ["junior", "middle", "senior", "не указан"]
                    },
                    "key_responsibilities": {
                      "type": "array",
                      "description": "Ключевые обязанности из описания вакансии",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "required_skills": {
                      "type": "object",
                      "description": "Обязательные требования (hard skills), опыт и технологии",
                      "additionalProperties": false,
                      "required": ["technical", "technologies", "experience"],
                      "properties": {
                        "technical": {
                          "type": "array",
                          "description": "Технические навыки/требования (группировать схожие, без дубликатов на уровне генерации)",
                          "items": {
                            "type": "string",
                            "minLength": 1
                          }
                        },
                        "technologies": {
                          "type": "array",
                          "description": "Используемые технологии/инструменты (ЯП, фреймворки, платформы, системы)",
                          "items": {
                            "type": "string",
                            "minLength": 1
                          }
                        },
                        "experience": {
                          "type": "string",
                          "description": "Требуемый опыт работы (например: '3+ года в backend', 'опыт с Kubernetes')"
                        }
                      }
                    },
                    "optional_skills": {
                      "type": "array",
                      "description": "Желательные требования (nice-to-have)",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "soft_skills": {
                      "type": "array",
                      "description": "Soft skills, явно указанные в вакансии",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "domain_knowledge": {
                      "type": "array",
                      "description": "Знание предметной области (финтех, e-commerce, healthtech и т.д.), если указано",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "interview_focus_areas": {
                      "type": "array",
                      "description": "Ключевые области для проверки на интервью",
                      "items": {
                        "type": "string",
                        "minLength": 1
                      }
                    },
                    "company_specific": {
                      "type": "object",
                      "description": "Специфические требования/контекст компании",
                      "additionalProperties": false,
                      "required": ["industry", "product", "team_size", "methodology"],
                      "properties": {
                        "industry": {
                          "type": "string",
                          "description": "Отрасль компании"
                        },
                        "product": {
                          "type": "string",
                          "description": "Продукт/сервис компании"
                        },
                        "team_size": {
                          "type": "string",
                          "description": "Размер команды или диапазон"
                        },
                        "methodology": {
                          "type": "string",
                          "description": "Методология работы (Agile/Scrum/Kanban/другое)"
                        }
                      }
                    }
                  }
                }
            """;

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);
    private static final String API_BASE_URL = "https://api.hh.ru";

    private final RestClient restClient = RestClient.builder()
            .baseUrl(API_BASE_URL)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Accept-Language", "ru_RU")
            .defaultHeader("User-Agent", "ai-interview-backend/0.0.1")
            .build();

    private final ChatClient chatClient;

    public VacancyService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .responseFormat(ResponseFormat.builder()
                                .type(ResponseFormat.Type.JSON_SCHEMA)
                                .jsonSchema(ResponseFormat.JsonSchema.builder()
                                        .name("vacancy")
                                        .schema(VACANCY_OUTPUT_SCHEMA)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public String fetchVacancy(String vacancyUrl) {
        if (vacancyUrl == null || vacancyUrl.isBlank()) {
            throw new IllegalArgumentException("vacancyUrl must be provided");
        }

        String vacancyId = extractVacancyId(vacancyUrl);
        if (vacancyId == null) {
            throw new IllegalArgumentException("Invalid vacancyUrl, cannot extract id: " + vacancyUrl);
        }

        String endpoint = "/vacancies/" + vacancyId;
        try {
            String response = restClient.get()
                    .uri(URI.create(API_BASE_URL + endpoint))
                    .retrieve()
                    .body(String.class);

            if (response == null) {
                throw new NotFoundException("Vacancy not found: id=" + vacancyId);
            }

            return chatClient
                    .prompt()
                    .user(Prompts.getParseVacancyPrompt(response))
                    .call()
                    .content();

        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Vacancy not found: id=" + vacancyId);
        } catch (HttpClientErrorException e) {
            log.error("HH API error: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private String extractVacancyId(String vacancyUrl) {
        try {
            URI uri = URI.create(vacancyUrl);
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            // Expected patterns: /vacancy/{id}, possibly with trailing slash
            String[] segments = path.split("/");
            for (int i = 0; i < segments.length - 1; i++) {
                if ("vacancy".equalsIgnoreCase(segments[i])) {
                    String candidate = segments[i + 1];
                    if (candidate != null && candidate.matches("\\d+")) {
                        return candidate;
                    }
                }
            }
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
