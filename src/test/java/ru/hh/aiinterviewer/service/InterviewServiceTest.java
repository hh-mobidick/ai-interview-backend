package ru.hh.aiinterviewer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallPromptResponseSpec;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.hh.aiinterviewer.api.dto.MessageRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageResponseDto;
import ru.hh.aiinterviewer.domain.model.Session;
import ru.hh.aiinterviewer.domain.model.SessionStatus;
import ru.hh.aiinterviewer.domain.repository.SessionRepository;
import ru.hh.aiinterviewer.exception.SessionCompletedException;

public class InterviewServiceTest {

  private VacancyService vacancyService;
  private SessionRepository sessionRepository;
  private ChatClient interviewerClient;
  private ChatClient preparePlanClient;
  private TranscriptionService transcriptionService;
  private InterviewService interviewService;
  private UUID sessionId;
  private Session session;

  @BeforeEach
  void setup() {
    vacancyService = mock(VacancyService.class);
    sessionRepository = mock(SessionRepository.class);

    interviewerClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    preparePlanClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
    transcriptionService = mock(TranscriptionService.class);

    interviewService = new InterviewService(
        vacancyService,
        sessionRepository,
        interviewerClient,
        preparePlanClient,
        transcriptionService
    );

    sessionId = UUID.randomUUID();
    session = Session.builder()
        .id(sessionId)
        .status(SessionStatus.PLANNED)
        .interviewPlan("OLD PLAN")
        .build();
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
    when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void planned_nonStart_message_updates_plan_and_stays_planned() {
    // Arrange: plan regeneration returns NEW PLAN
    when(preparePlanClient.prompt().system(any(String.class)).call().content()).thenReturn("NEW PLAN");
    MessageRequestDto req = MessageRequestDto.builder().type("text").message("исправь план").build();

    // Act
    MessageResponseDto resp = interviewService.processMessage(sessionId, req);

    // Assert
    assertThat(resp.getMessage()).isEqualTo("NEW PLAN");
    assertThat(session.getStatus()).isEqualTo(SessionStatus.PLANNED);
    assertThat(session.getInterviewPlan()).isEqualTo("NEW PLAN");
  }

  @Test
  void planned_start_transitions_to_ongoing() {
    when(interviewerClient.prompt().system(any(String.class)).user(any(String.class))
        .advisors(Mockito.<Consumer>any()).call().content())
        .thenReturn("Вопрос 1/5 (Тема: X): ...?");
    MessageRequestDto req = MessageRequestDto.builder().type("text").message("Начать интервью").build();

    MessageResponseDto resp = interviewService.processMessage(sessionId, req);

    assertThat(resp.isInterviewComplete()).isFalse();
    assertThat(session.getStatus()).isEqualTo(SessionStatus.ONGOING);
  }

  @Test
  void completed_session_throws_410() {
    session.setStatus(SessionStatus.COMPLETED);
    MessageRequestDto req = MessageRequestDto.builder().type("text").message("hi").build();
    assertThatThrownBy(() -> interviewService.processMessage(sessionId, req))
        .isInstanceOf(SessionCompletedException.class);
  }
}


