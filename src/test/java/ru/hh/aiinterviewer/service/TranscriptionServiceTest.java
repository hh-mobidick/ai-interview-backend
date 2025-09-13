package ru.hh.aiinterviewer.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import ru.hh.aiinterviewer.config.ApplicationProperties;

public class TranscriptionServiceTest {

  private TranscriptionService transcriptionService;

  @BeforeEach
  void setup() {
    OpenAiAudioTranscriptionModel model = mock(OpenAiAudioTranscriptionModel.class);
    ApplicationProperties props = new ApplicationProperties();
    transcriptionService = new TranscriptionService(model, props);
  }

  @Test
  void rejectNonWavBytes() {
    String base64 = java.util.Base64.getEncoder().encodeToString(new byte[]{0,1,2,3,4});
    assertThatThrownBy(() -> transcriptionService.transcribe(base64))
        .isInstanceOf(ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException.class);
  }
}


