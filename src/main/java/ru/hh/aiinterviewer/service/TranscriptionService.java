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
  private static final long MAX_WAV_SIZE_BYTES = 25L * 1024 * 1024; // 25MB safety limit
  private static final int MIN_SAMPLE_RATE = 8000;
  private static final int MAX_SAMPLE_RATE = 48000;

  private final OpenAiAudioTranscriptionModel audioTranscriptionModel;

  public String transcribe(String audioBase64) {
    if (audioBase64 == null || audioBase64.isBlank()) {
      throw new IllegalArgumentException("audioBase64 must not be blank");
    }

    byte[] audioBytes = Base64.getDecoder().decode(audioBase64.getBytes(StandardCharsets.UTF_8));
    validateWav(audioBytes);
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

  private void validateWav(byte[] bytes) {
    if (bytes.length < 44) {
      throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("WAV too small or invalid");
    }
    if (bytes.length > MAX_WAV_SIZE_BYTES) {
      throw new ru.hh.aiinterviewer.exception.FileTooLargeException("Audio file too large");
    }
    // Check RIFF header and WAVE
    if (!(bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F')) {
      throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Not a RIFF file");
    }
    if (!(bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E')) {
      throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Not a WAVE file");
    }

    boolean fmtFound = false;
    boolean dataFound = false;
    int channels = -1;
    int sampleRate = -1;

    int i = 12;
    while (i + 8 <= bytes.length) {
      String id = new String(bytes, i, 4, StandardCharsets.US_ASCII);
      int chunkSize = le32(bytes, i + 4);
      int dataStart = i + 8;
      if (dataStart + chunkSize > bytes.length) {
        break;
      }
      if ("fmt ".equals(id)) {
        fmtFound = true;
        if (chunkSize >= 16) {
          int audioFormat = le16(bytes, dataStart);
          channels = le16(bytes, dataStart + 2);
          sampleRate = le32(bytes, dataStart + 4);
          int bitsPerSample = (chunkSize >= 16) ? le16(bytes, dataStart + 14) : -1;
          if (audioFormat != 1 && audioFormat != 3) { // PCM or IEEE float
            throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Unsupported WAV format");
          }
          if (channels < 1 || channels > 2) {
            throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Unsupported channel count");
          }
          if (sampleRate < MIN_SAMPLE_RATE || sampleRate > MAX_SAMPLE_RATE) {
            throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Unsupported sample rate");
          }
          if (bitsPerSample != -1 && bitsPerSample % 8 != 0) {
            throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("Unsupported bits per sample");
          }
        }
      } else if ("data".equals(id)) {
        dataFound = chunkSize > 0;
      }
      // Chunks are word-aligned; if odd, skip padding byte
      i = dataStart + chunkSize + (chunkSize % 2);
    }

    if (!fmtFound || !dataFound) {
      throw new ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException("WAV missing fmt or data chunk");
    }
  }

  private int le16(byte[] b, int off) {
    return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
  }

  private int le32(byte[] b, int off) {
    return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
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
