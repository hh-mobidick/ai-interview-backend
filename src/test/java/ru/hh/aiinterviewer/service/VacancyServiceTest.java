package ru.hh.aiinterviewer.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import ru.hh.aiinterviewer.config.ApplicationProperties;

public class VacancyServiceTest {

  private VacancyService vacancyService;

  @BeforeEach
  void setup() {
    vacancyService = new VacancyService(new ApplicationProperties());
  }

  @Test
  void rejectEmptyFile() {
    MultipartFile file = mock(MultipartFile.class);
    org.mockito.Mockito.when(file.isEmpty()).thenReturn(true);
    assertThatThrownBy(() -> vacancyService.extractTextFromFile(file))
        .isInstanceOf(IllegalArgumentException.class);
  }
}


