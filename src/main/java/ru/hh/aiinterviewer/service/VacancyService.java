package ru.hh.aiinterviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;

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

            return response;

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
