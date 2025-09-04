package ru.hh.aiinterviewer.service.dto;

import java.util.List;

public record VacancyInfo(
    String id,
    String title,
    String description,
    String url,
    String employer,
    String experienceLevel,
    List<String> keySkills,
    List<String> professionalRoles,
    List<String> specializations,
    Snippet snippet
) {
  public record Snippet(String requirements, String responsibilities) {}
}
