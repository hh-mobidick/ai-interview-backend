package ru.hh.aiinterviewer.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public class ApplicationProperties {

  @Positive
  private long maxFileSizeBytes = 5L * 1024 * 1024; // 5 MB default

  @Positive
  private long maxAudioSizeBytes = 25L * 1024 * 1024; // 25 MB default

  @Min(8000)
  @Max(96000)
  private int minAudioSampleRate = 8000;

  @Min(8000)
  @Max(192000)
  private int maxAudioSampleRate = 48000;

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }

  public long getMaxAudioSizeBytes() {
    return maxAudioSizeBytes;
  }

  public void setMaxAudioSizeBytes(long maxAudioSizeBytes) {
    this.maxAudioSizeBytes = maxAudioSizeBytes;
  }

  public int getMinAudioSampleRate() {
    return minAudioSampleRate;
  }

  public void setMinAudioSampleRate(int minAudioSampleRate) {
    this.minAudioSampleRate = minAudioSampleRate;
  }

  public int getMaxAudioSampleRate() {
    return maxAudioSampleRate;
  }

  public void setMaxAudioSampleRate(int maxAudioSampleRate) {
    this.maxAudioSampleRate = maxAudioSampleRate;
  }
}
