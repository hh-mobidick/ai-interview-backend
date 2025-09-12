package ru.hh.aiinterviewer.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStatusResponseDto {

  private String status;
}
