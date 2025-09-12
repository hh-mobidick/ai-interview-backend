package ru.hh.aiinterviewer.api;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.hh.aiinterviewer.api.dto.ApiError;
import ru.hh.aiinterviewer.api.dto.ApiErrorCode;
import ru.hh.aiinterviewer.exception.FileTooLargeException;
import ru.hh.aiinterviewer.exception.FileTypeNotSupportedException;
import ru.hh.aiinterviewer.exception.InvalidStatusTransitionException;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.exception.SessionCompletedException;
import ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException;
import ru.hh.aiinterviewer.exception.VacancyNotParsableException;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiError.builder()
            .code(ApiErrorCode.NOT_FOUND.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getAllErrors().stream()
        .map(error -> error.getDefaultMessage())
        .toList();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.INVALID_INPUT.getCode())
            .message("Validation error")
            .details(Map.of("errors", errors))
            .build());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.INVALID_INPUT.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(InvalidStatusTransitionException.class)
  public ResponseEntity<ApiError> handleInvalidTransition(InvalidStatusTransitionException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.builder()
            .code(ApiErrorCode.INVALID_STATUS_TRANSITION.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(SessionCompletedException.class)
  public ResponseEntity<ApiError> handleSessionCompleted(SessionCompletedException ex) {
    return ResponseEntity.status(HttpStatus.GONE)
        .body(ApiError.builder()
            .code(ApiErrorCode.SESSION_COMPLETED.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(VacancyNotParsableException.class)
  public ResponseEntity<ApiError> handleVacancyNotParsable(VacancyNotParsableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.VACANCY_NOT_PARSABLE.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(FileTypeNotSupportedException.class)
  public ResponseEntity<ApiError> handleFileTypeNotSupported(FileTypeNotSupportedException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.FILE_TYPE_NOT_SUPPORTED.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(FileTooLargeException.class)
  public ResponseEntity<ApiError> handleFileTooLarge(FileTooLargeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.FILE_TOO_LARGE.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(UnsupportedAudioFormatException.class)
  public ResponseEntity<ApiError> handleUnsupportedAudio(UnsupportedAudioFormatException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.builder()
            .code(ApiErrorCode.UNSUPPORTED_AUDIO_FORMAT.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    if (message.contains("completed")) {
      return ResponseEntity.status(HttpStatus.GONE)
          .body(ApiError.builder()
              .code(ApiErrorCode.SESSION_COMPLETED.getCode())
              .message(ex.getMessage())
              .build());
    }
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiError.builder()
            .code(ApiErrorCode.INVALID_STATUS_TRANSITION.getCode())
            .message(ex.getMessage())
            .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.builder()
            .code(ApiErrorCode.INTERNAL_ERROR.getCode())
            .message("Unexpected error")
            .build());
  }
}
