package ru.hh.aiinterviewer.service;

import java.io.IOException;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.hh.aiinterviewer.service.dto.VacancyInfo;

@Service
public class VacancyService {

  private static final Logger log = LoggerFactory.getLogger(VacancyService.class);

  public VacancyInfo fetchVacancy(String vacancyUrl) {
    Objects.requireNonNull(vacancyUrl, "vacancyUrl must not be null");
    try {
      Document doc = Jsoup.connect(vacancyUrl)
          .userAgent("Mozilla/5.0 (compatible; ai-interviewer/1.0; +https://example.com)")
          .referrer("https://hh.ru/")
          .timeout(15_000)
          .get();

      String title = extractTitle(doc);
      String summary = extractSummary(doc);
      return new VacancyInfo(title, summary, vacancyUrl);
    } catch (IOException e) {
      log.warn("Failed to fetch or parse vacancy from URL: {} - {}", vacancyUrl, e.getMessage());
      return new VacancyInfo(null, null, vacancyUrl);
    }
  }

  private String extractTitle(Document doc) {
    // Try specific hh.ru selectors first, then fallback to <title>
    Element header = doc.selectFirst("h1[data-qa='vacancy-title'], h1.bloko-header-1");
    if (header != null && !header.text().isBlank()) {
      return header.text().trim();
    }
    String title = doc.title();
    return title == null ? null : title.trim();
  }

  private String extractSummary(Document doc) {
    // Try to collect key sections like responsibilities/requirements text
    StringBuilder sb = new StringBuilder();
    Elements sections = doc.select("div[data-qa='vacancy-description'], div.vacancy-section");
    for (Element el : sections) {
      String text = el.text();
      if (text != null && !text.isBlank()) {
        if (sb.length() > 0) sb.append(" \n");
        sb.append(text.trim());
      }
      if (sb.length() > 1500) {
        break;
      }
    }
    String summary = sb.toString();
    if (summary.isBlank()) {
      // fallback to meta description
      Element meta = doc.selectFirst("meta[name=description]");
      if (meta != null) {
        String content = meta.attr("content");
        if (content != null && !content.isBlank()) {
          summary = content.trim();
        }
      }
    }
    return summary.isBlank() ? null : summary;
  }

}


