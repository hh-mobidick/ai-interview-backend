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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranscriptionService {

  private static final String MODEL = "whisper-1";

  private final OpenAiAudioTranscriptionModel audioTranscriptionModel;

  public String transcribe(String audioBase64, String audioMimeType) {
    if (audioBase64 == null || audioBase64.isBlank()) {
      throw new IllegalArgumentException("audioBase64 must not be blank");
    }
    if (audioMimeType == null || audioMimeType.isBlank()) {
      throw new IllegalArgumentException("audioMimeType must not be blank");
    }

    byte[] audioBytes = Base64.getDecoder().decode(audioBase64.getBytes(StandardCharsets.UTF_8));
    String filename = "audio." + mapMimeToExtension(audioMimeType);
    Resource resource = new NamedByteArrayResource(audioBytes, filename);

    OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
        .model(MODEL)
        .temperature(0f)
        .build();

    AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
    AudioTranscriptionResponse response = audioTranscriptionModel.call(prompt);

    String text = response.getResult().getOutput();
    if (text == null || text.isBlank()) {
      throw new IllegalStateException("Failed to transcribe audio");
    }
    return text;
  }

  private String mapMimeToExtension(String mime) {
    Map<String, String> map = new HashMap<>();
    map.put("audio/mpeg", "mp3");
    map.put("audio/mp3", "mp3");
    map.put("audio/wav", "wav");
    map.put("audio/x-wav", "wav");
    map.put("audio/webm", "webm");
    map.put("audio/ogg", "ogg");
    map.put("audio/flac", "flac");
    map.put("audio/mp4", "mp4");
    map.put("audio/aac", "aac");
    return map.getOrDefault(mime, "bin");
  }

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
