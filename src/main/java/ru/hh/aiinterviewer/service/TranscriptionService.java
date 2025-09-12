package ru.hh.aiinterviewer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

  private static final String MODEL = "gpt-4o-mini-transcribe";

  private final OpenAiAudioTranscriptionModel audioTranscriptionModel;

  public String transcribe(String audioBase64, String audioMimeType) {
    if (audioBase64 == null || audioBase64.isBlank()) {
      throw new IllegalArgumentException("audioBase64 must not be blank");
    }
    // audioMimeType is deprecated; only WAV is supported now

    byte[] audioBytes = Base64.getDecoder().decode(audioBase64.getBytes(StandardCharsets.UTF_8));
    String filename = "audio.wav";
    Resource resource = new NamedByteArrayResource(audioBytes, filename);

    OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
        .model(MODEL)
        .temperature(0f)
        .build();

    AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
    AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);

    String text = response.getResult().getOutput();
    if (text.isBlank()) {
      throw new IllegalStateException("Failed to transcribe audio");
    }
    return text;
  }

  // No mime-type mapping; only WAV is supported now.

  private static class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;

    public NamedByteArrayResource(byte[] byteArray, String filename) {
      super(byteArray);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
