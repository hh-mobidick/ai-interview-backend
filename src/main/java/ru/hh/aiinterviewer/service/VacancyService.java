package ru.hh.aiinterviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;
import ru.hh.aiinterviewer.exception.NotFoundException;

@Service
public class VacancyService {

  private static final Logger log = LoggerFactory.getLogger(VacancyService.class);
  private static final String API_BASE_URL = "https://api.hh.ru";

  private final RestClient restClient = RestClient.builder()
      .baseUrl(API_BASE_URL)
      .defaultHeader("Accept", "application/json")
      .defaultHeader("Accept-Language", "ru_RU")
      .defaultHeader("User-Agent", "ai-interview-backend/0.0.1")
      .build();

  public VacancyInfo fetchVacancy(String vacancyUrl) {
    if (vacancyUrl == null || vacancyUrl.isBlank()) {
      throw new IllegalArgumentException("vacancyUrl must be provided");
    }

    String vacancyId = extractVacancyId(vacancyUrl);
    if (vacancyId == null) {
      throw new IllegalArgumentException("Invalid vacancyUrl, cannot extract id: " + vacancyUrl);
    }

    String endpoint = "/vacancies/" + vacancyId;
    try {
      JsonNode root = restClient.get()
          .uri(URI.create(API_BASE_URL + endpoint))
          .retrieve()
          .body(JsonNode.class);

      if (root == null) {
        throw new NotFoundException("Vacancy not found: id=" + vacancyId);
      }

      String id = getText(root, "id");
      String title = getText(root, "name");
      String descriptionHtml = getText(root, "description");

      String employerName = null;
      JsonNode employerNode = root.path("employer");
      if (!employerNode.isMissingNode()) {
        employerName = getText(employerNode, "name");
      }

      String experienceLevel = null;
      JsonNode experienceNode = root.path("experience");
      if (!experienceNode.isMissingNode()) {
        experienceLevel = getText(experienceNode, "name");
      }

      List<String> skills = new ArrayList<>();
      JsonNode keySkillsNode = root.path("key_skills");
      if (keySkillsNode.isArray()) {
        for (JsonNode n : keySkillsNode) {
          String name = getText(n, "name");
          if (name != null && !name.isBlank()) {
            skills.add(name);
          }
        }
      }

      List<String> roles = new ArrayList<>();
      JsonNode rolesNode = root.path("professional_roles");
      if (rolesNode.isArray()) {
        for (JsonNode n : rolesNode) {
          String name = getText(n, "name");
          if (name != null && !name.isBlank()) {
            roles.add(name);
          }
        }
      }

      List<String> specializations = new ArrayList<>();
      JsonNode specsNode = root.path("specializations");
      if (specsNode.isArray()) {
        for (JsonNode n : specsNode) {
          String name = getText(n, "name");
          if (name != null && !name.isBlank()) {
            specializations.add(name);
          }
        }
      }

      String requirements = null;
      String responsibilities = null;
      JsonNode snippetNode = root.path("snippet");
      if (!snippetNode.isMissingNode()) {
        requirements = getText(snippetNode, "requirement");
        responsibilities = getText(snippetNode, "responsibility");
      }

      // Ensure required fields are present
      if (title == null || title.isBlank()) {
        throw new IllegalStateException("Vacancy has no title: id=" + vacancyId);
      }
      if (descriptionHtml == null) {
        descriptionHtml = "";
      }

      VacancyInfo.Snippet snippet = new VacancyInfo.Snippet(requirements, responsibilities);

      return new VacancyInfo(
          id,
          title,
          descriptionHtml,
          vacancyUrl,
          employerName,
          experienceLevel,
          java.util.List.copyOf(skills),
          java.util.List.copyOf(roles),
          java.util.List.copyOf(specializations),
          snippet
      );
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

  private String getText(JsonNode node, String field) {
    if (node == null || field == null) {
      return null;
    }
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText(null);
    return text == null || text.isBlank() ? null : text;
  }
}


