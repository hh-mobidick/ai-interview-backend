package ru.hh.aiinterviewer.api;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.hh.aiinterviewer.exception.NotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.NOT_FOUND.value(),
            "error", "Not Found",
            "message", ex.getMessage()
        ));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", ex.getBindingResult().getAllErrors().isEmpty() ? "Validation error" : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage()
        ));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", ex.getMessage()
        ));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
    // 410 Gone for completed sessions
    if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("completed")) {
      return ResponseEntity.status(HttpStatus.GONE)
          .body(Map.of(
              "timestamp", Instant.now().toString(),
              "status", HttpStatus.GONE.value(),
              "error", "Gone",
              "message", ex.getMessage()
          ));
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.BAD_REQUEST.value(),
            "error", "Bad Request",
            "message", ex.getMessage()
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error", "Internal Server Error",
            "message", "Unexpected error"
        ));
  }
}
