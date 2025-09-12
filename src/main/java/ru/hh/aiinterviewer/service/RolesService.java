package ru.hh.aiinterviewer.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RolesService {

  private static final List<String> ROLES = Arrays.asList(
      "Backend Developer (Java)",
      "Backend Developer (Go)",
      "Backend Developer (Node.js)",
      "Frontend Developer (React)",
      "Fullstack Developer (React + Java)",
      "Mobile Developer (iOS)",
      "Mobile Developer (Android)",
      "Data Engineer",
      "Data Scientist",
      "ML Engineer",
      "DevOps Engineer",
      "Site Reliability Engineer",
      "QA Engineer",
      "Automation QA Engineer",
      "Product Manager",
      "Project Manager",
      "Business Analyst",
      "System Analyst",
      "UX/UI Designer"
  );

  public List<String> suggest(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String q = query.toLowerCase(Locale.ROOT);
    return ROLES.stream()
        .filter(r -> r.toLowerCase(Locale.ROOT).contains(q))
        .limit(20)
        .collect(Collectors.toList());
  }
}
