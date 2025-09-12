package ru.hh.aiinterviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import ru.hh.aiinterviewer.config.ApplicationProperties;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.exception.FileTooLargeException;
import ru.hh.aiinterviewer.exception.FileTypeNotSupportedException;
import ru.hh.aiinterviewer.exception.VacancyNotParsableException;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;
import ru.hh.aiinterviewer.utils.JsonUtils;

@Service
@RequiredArgsConstructor
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
    private final RestClient genericClient = RestClient.create();
    private final ApplicationProperties applicationProperties;

    public String getVacancy(String vacancyUrl) {
        return getVacancyByUrl(vacancyUrl);
    }

    public String getVacancyByUrl(String vacancyUrl) {
        if (vacancyUrl == null || vacancyUrl.isBlank()) {
            throw new IllegalArgumentException("vacancyUrl must be provided");
        }

        vacancyUrl = vacancyUrl.trim();

        // Try HH-specific path first
        String vacancyId = extractVacancyId(vacancyUrl);
        if (vacancyId != null) {
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
                throw new VacancyNotParsableException("Vacancy not found for url: " + vacancyUrl);
            } catch (HttpClientErrorException e) {
                log.error("HH API error: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
                throw e;
            }
        }

        // Generic URL handler (non-hh.ru): try to fetch and extract description/text
        return parseGenericVacancyPage(vacancyUrl);
    }

    private String parseGenericVacancyPage(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new VacancyNotParsableException("Unsupported URL scheme: " + url);
        }
        try {
            String html = genericClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);

            if (html == null || html.isBlank()) {
                throw new VacancyNotParsableException("Empty response from url: " + url);
            }

            String meta = extractMetaDescription(html);
            String text = meta != null ? meta : extractPlainText(html);
            if (text == null || text.isBlank()) {
                throw new VacancyNotParsableException("Cannot extract vacancy description from url: " + url);
            }
            return text;
        } catch (HttpClientErrorException e) {
            if (e instanceof HttpClientErrorException.NotFound) {
                throw new VacancyNotParsableException("Vacancy not found for url: " + url);
            }
            log.warn("Generic URL fetch failed: status={}, url={}", e.getStatusCode().value(), url);
            throw new VacancyNotParsableException("Failed to fetch vacancy from url: " + url);
        } catch (Exception e) {
            log.warn("Generic URL parse error for url {}", url, e);
            throw new VacancyNotParsableException("Failed to parse vacancy from url: " + url);
        }
    }

    private String extractMetaDescription(String html) {
        // Try common meta tags: description or og:description
        Pattern metaDesc = Pattern.compile("(?is)<meta\\s+name=\\\"description\\\"\\s+content=\\\"(.*?)\\\"\\s*/?>");
        Matcher m1 = metaDesc.matcher(html);
        if (m1.find()) {
            return sanitizeText(m1.group(1));
        }
        Pattern ogDesc = Pattern.compile("(?is)<meta\\s+property=\\\"og:description\\\"\\s+content=\\\"(.*?)\\\"\\s*/?>");
        Matcher m2 = ogDesc.matcher(html);
        if (m2.find()) {
            return sanitizeText(m2.group(1));
        }
        return null;
    }

    private String extractPlainText(String html) {
        String withoutScripts = html.replaceAll("(?is)<script.*?>.*?</script>", " ")
            .replaceAll("(?is)<style.*?>.*?</style>", " ");
        String text = withoutScripts.replaceAll("(?is)<[^>]+>", " ");
        text = sanitizeText(text);
        // Keep a reasonable length
        if (text.length() > 4000) {
            text = text.substring(0, 4000);
        }
        return text;
    }

    private String sanitizeText(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("\\s+", " ").trim();
    }

    public String extractTextFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("vacancyFile must not be empty");
        }
        long maxSizeBytes = applicationProperties.getMaxFileSizeBytes();
        if (file.getSize() > maxSizeBytes) {
            throw new FileTooLargeException("File too large, max is 5MB");
        }

        String filename = file.getOriginalFilename();
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".txt")) {
            try {
                byte[] bytes = file.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to read text file");
            }
        }
        if (lower.endsWith(".pdf") || lower.endsWith(".docx")) {
            return parseWithTika(file);
        }
        throw new FileTypeNotSupportedException("Unsupported file type");
    }

    private String parseWithTika(MultipartFile file) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> handlerCls = Class.forName("org.apache.tika.sax.BodyContentHandler", true, cl);
            Class<?> metadataCls = Class.forName("org.apache.tika.metadata.Metadata", true, cl);
            Class<?> contextCls = Class.forName("org.apache.tika.parser.ParseContext", true, cl);
            Class<?> parserCls = Class.forName("org.apache.tika.parser.AutoDetectParser", true, cl);

            Object handler = handlerCls.getConstructor(int.class).newInstance(-1);
            Object metadata = metadataCls.getConstructor().newInstance();
            Object context = contextCls.getConstructor().newInstance();
            Object parser = parserCls.getConstructor().newInstance();

            parserCls.getMethod("parse", java.io.InputStream.class, Class.forName("org.xml.sax.ContentHandler", true, cl), metadataCls, contextCls)
                .invoke(parser, file.getInputStream(), handler, metadata, context);

            String text = sanitizeText((String) handlerCls.getMethod("toString").invoke(handler));
            if (text == null || text.isBlank()) {
                throw new VacancyNotParsableException("Empty content in file");
            }
            if (text.length() > 10000) {
                text = text.substring(0, 10000);
            }
            return text;
        } catch (Exception e) {
            log.warn("Tika parse failed for file {}", file.getOriginalFilename(), e);
            throw new VacancyNotParsableException("Failed to parse file content");
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
