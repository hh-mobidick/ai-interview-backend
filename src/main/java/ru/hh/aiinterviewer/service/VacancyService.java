package ru.hh.aiinterviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;
import ru.hh.aiinterviewer.utils.JsonUtils;

@Service
public class VacancyService {

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);
    private static final String API_BASE_URL = "https://api.hh.ru";
    private static final Pattern VACANCY_PATH_PATTERN = Pattern.compile("(?i)/vacancy/(\\d+)(?:/|$)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
            .baseUrl(API_BASE_URL)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Accept-Language", "ru_RU")
            .defaultHeader("User-Agent", "ai-interview-backend/0.0.1")
            .build();

    public String getVacancy(String vacancyUrl) {
        if (vacancyUrl == null || vacancyUrl.isBlank()) {
            throw new IllegalArgumentException("vacancyUrl must be provided");
        }

        String vacancyId = extractVacancyId(vacancyUrl);
        if (vacancyId == null) {
            throw new IllegalArgumentException("Invalid vacancyUrl, cannot extract id: " + vacancyUrl);
        }
        try {
            String response = restClient.get()
                    .uri(URI.create(API_BASE_URL + "/vacancies/" + vacancyId))
                    .retrieve()
                    .body(String.class);

            if (response == null) {
                throw new NotFoundException("Vacancy not found: id=" + vacancyId);
            }

            return filterVacancyJson(response);

        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Vacancy not found: id=" + vacancyId);
        } catch (HttpClientErrorException e) {
            log.error("HH API error: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw e;
        }
    }

    private String filterVacancyJson(String rawJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);

            String id = getTextOrNull(root, "id");
            String title = getTextOrNull(root, "name");
            String description = getTextOrNull(root, "description");
            String url = getTextOrNull(root, "alternate_url");
            String employer = getTextOrNull(root.path("employer"), "name");
            String experienceLevel = getTextOrNull(root.path("experience"), "name");

            List<String> keySkills = extractNamesArray(root.path("key_skills"), "name");
            List<String> professionalRoles = extractNamesArray(root.path("professional_roles"), "name");
            List<String> specializations = extractNamesArray(root.path("specializations"), "name");

            VacancyInfo.Snippet snippet = null;
            JsonNode snippetNode = root.path("snippet");
            if (!snippetNode.isMissingNode() && !snippetNode.isNull()) {
                String requirements = getTextOrNull(snippetNode, "requirement");
                String responsibilities = getTextOrNull(snippetNode, "responsibility");
                if (requirements != null || responsibilities != null) {
                    snippet = new VacancyInfo.Snippet(requirements, responsibilities);
                }
            }

            VacancyInfo info = new VacancyInfo(
                id,
                title,
                description,
                url,
                employer,
                experienceLevel,
                keySkills,
                professionalRoles,
                specializations,
                snippet
            );

            return JsonUtils.toJson(info);
        } catch (Exception e) {
            log.warn("Failed to filter vacancy JSON, returning original.", e);
            return rawJson;
        }
    }

    private List<String> extractNamesArray(JsonNode arrayNode, String fieldName) {
        List<String> values = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String value = getTextOrNull(item, fieldName);
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String extractVacancyId(String vacancyUrl) {
        try {
            URI uri = URI.create(vacancyUrl);
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            Matcher matcher = VACANCY_PATH_PATTERN.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }

            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
