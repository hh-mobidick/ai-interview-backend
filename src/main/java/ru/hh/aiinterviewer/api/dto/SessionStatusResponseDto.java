package ru.hh.aiinterviewer.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStatusResponseDto {

  private String status;
}
