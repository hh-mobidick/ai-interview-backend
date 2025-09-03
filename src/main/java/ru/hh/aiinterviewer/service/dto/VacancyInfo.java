package ru.hh.aiinterviewer.service.dto;

import lombok.NonNull;

public record VacancyInfo(@NonNull String title, @NonNull String description, String url) {
}
