package ru.hh.aiinterviewer.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import ru.hh.aiinterviewer.api.dto.ApiError;
import ru.hh.aiinterviewer.api.dto.ApiErrorCode;
import ru.hh.aiinterviewer.exception.FileTooLargeException;
import ru.hh.aiinterviewer.exception.FileTypeNotSupportedException;
import ru.hh.aiinterviewer.exception.InvalidStatusTransitionException;
import ru.hh.aiinterviewer.exception.NotFoundException;
import ru.hh.aiinterviewer.exception.SessionCompletedException;
import ru.hh.aiinterviewer.exception.UnsupportedAudioFormatException;
import ru.hh.aiinterviewer.exception.VacancyNotParsableException;

public class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void notFound_mapsTo404() {
    var resp = handler.handleNotFound(new NotFoundException("nope"));
    assertThat(resp.getStatusCode().value()).isEqualTo(404);
    ApiError body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCode()).isEqualTo(ApiErrorCode.NOT_FOUND.getCode());
  }

  @Test
  void invalidTransition_mapsTo409() {
    var resp = handler.handleInvalidTransition(new InvalidStatusTransitionException("bad"));
    assertThat(resp.getStatusCode().value()).isEqualTo(409);
    ApiError body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCode()).isEqualTo(ApiErrorCode.INVALID_STATUS_TRANSITION.getCode());
  }

  @Test
  void sessionCompleted_mapsTo410() {
    var resp = handler.handleSessionCompleted(new SessionCompletedException("done"));
    assertThat(resp.getStatusCode().value()).isEqualTo(410);
    ApiError body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCode()).isEqualTo(ApiErrorCode.SESSION_COMPLETED.getCode());
  }

  @Test
  void vacancyNotParsable_mapsTo400() {
    var resp = handler.handleVacancyNotParsable(new VacancyNotParsableException("bad url"));
    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    ApiError body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCode()).isEqualTo(ApiErrorCode.VACANCY_NOT_PARSABLE.getCode());
  }

  @Test
  void fileIssues_mapTo400() {
    var resp1 = handler.handleFileTypeNotSupported(new FileTypeNotSupportedException("type"));
    var resp2 = handler.handleFileTooLarge(new FileTooLargeException("big"));
    var resp3 = handler.handleUnsupportedAudio(new UnsupportedAudioFormatException("fmt"));
    assertThat(resp1.getStatusCode().value()).isEqualTo(400);
    assertThat(resp2.getStatusCode().value()).isEqualTo(400);
    assertThat(resp3.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  void validation_mapsTo400WithInvalidInputCode() throws NoSuchMethodException {
    // Create a minimal MethodArgumentNotValidException
    Object target = new Object();
    var bindingResult = new BeanPropertyBindingResult(target, "target");
    bindingResult.addError(new ObjectError("field", "must not be blank"));
    Method m = GlobalExceptionHandlerTest.class.getDeclaredMethod("validation_mapsTo400WithInvalidInputCode");
    MethodParameter mp = new MethodParameter(m, -1);
    var ex = new MethodArgumentNotValidException(mp, bindingResult);

    var resp = handler.handleValidation(ex);
    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    ApiError body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCode()).isEqualTo(ApiErrorCode.INVALID_INPUT.getCode());
    Object details = body.getDetails();
    java.util.Map<?,?> map = (java.util.Map<?,?>) details;
    assertThat(((List<?>) map.get("errors")).size()).isGreaterThanOrEqualTo(1);
  }
}


