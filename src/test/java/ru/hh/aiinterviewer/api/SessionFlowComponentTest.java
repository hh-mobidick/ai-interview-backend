package ru.hh.aiinterviewer.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.hh.aiinterviewer.api.dto.CreateSessionRequestDto;
import ru.hh.aiinterviewer.api.dto.MessageRequestDto;
import ru.hh.aiinterviewer.domain.model.SessionMode;
import ru.hh.aiinterviewer.api.dto.SessionResponseDto;
import ru.hh.aiinterviewer.api.dto.SessionStatusResponseDto;
import ru.hh.aiinterviewer.domain.model.Role;
import ru.hh.aiinterviewer.domain.model.RoleSynonym;
import ru.hh.aiinterviewer.domain.repository.RoleRepository;
import ru.hh.aiinterviewer.domain.repository.RoleSynonymRepository;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SessionFlowComponentTest {

  @Container
  @ServiceConnection
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  // Triggers
  private static final String TR_PLAN = "План интервью";
  private static final String TR_START = "Начать интервью";
  private static final String TR_FEEDBACK = "Обратная связь";
  private static final String TR_FINISH = "Завершить интервью";
  private static final String SYS_COMPLETE = "Интервью завершено";

  // Answers
  private static final String A_PLAN = "План интервью";
  private static final String A_Q1 = "Вопрос 1/3";
  private static final String A_Q2 = "Вопрос 2/3";
  private static final String A_Q3 = "Вопрос 3/3";
  private static final String A_FEEDBACK_HEADER = "Обратная связь";
  private static final String A_FEEDBACK_BODY = "Ответ по обратной связи";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean(name = "interviewerChatClient", answer = Answers.RETURNS_DEEP_STUBS)
  private ChatClient interviewerChatClient;

  @MockBean(name = "prepareInterviewPlanChatClient", answer = Answers.RETURNS_DEEP_STUBS)
  private ChatClient prepareInterviewPlanChatClient;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private RoleSynonymRepository roleSynonymRepository;

  @BeforeEach
  void beforeEach() {
    // Default LLM responses for plan generation/correction
    org.mockito.Mockito.when(prepareInterviewPlanChatClient.prompt().user(anyString()).call().content())
        .thenReturn(A_PLAN);
    org.mockito.Mockito.when(prepareInterviewPlanChatClient.prompt().system(anyString()).call().content())
        .thenReturn(A_PLAN);
  }

  @Test
  void full_flow_plan_to_feedback_to_finish() throws Exception {
    String sessionId = createSession();

    sendText(sessionId, TR_PLAN)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_PLAN)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer(TR_START, A_Q1);
    sendText(sessionId, TR_START)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_Q1)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer("ответ1", A_Q2);
    sendText(sessionId, "ответ1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_Q2)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer("ответ2", A_Q3);
    sendText(sessionId, "ответ2")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_Q3)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer(TR_FEEDBACK, A_FEEDBACK_HEADER);
    sendText(sessionId, TR_FEEDBACK)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_FEEDBACK_HEADER)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer("мне всё понятно", A_FEEDBACK_BODY);
    sendText(sessionId, "мне всё понятно")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(A_FEEDBACK_BODY)))
        .andExpect(jsonPath("$.interviewComplete", is(false)));

    stubInterviewer(SYS_COMPLETE, SYS_COMPLETE);
    sendText(sessionId, TR_FINISH)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", containsString(SYS_COMPLETE)))
        .andExpect(jsonPath("$.interviewComplete", is(true)));

    sendText(sessionId, "ещё что-то").andExpect(status().isGone());
  }

  @Test
  void direct_finish_from_ongoing() throws Exception {
    String sessionId = createSession();
    stubInterviewer(TR_START, A_Q1);
    sendText(sessionId, TR_START).andExpect(status().isOk());

    stubInterviewer(SYS_COMPLETE, SYS_COMPLETE);
    sendText(sessionId, TR_FINISH)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interviewComplete", is(true)));
  }

  @Test
  void invalid_commands_in_planned() throws Exception {
    String sessionId = createSession();
    sendText(sessionId, TR_FEEDBACK).andExpect(status().isConflict());
    sendText(sessionId, TR_FINISH).andExpect(status().isConflict());
  }

  @Test
  void messages_after_completion_return_410() throws Exception {
    String sessionId = createSession();
    stubInterviewer(TR_START, A_Q1);
    sendText(sessionId, TR_START).andExpect(status().isOk());

    stubInterviewer(SYS_COMPLETE, SYS_COMPLETE);
    sendText(sessionId, TR_FINISH).andExpect(status().isOk());

    sendText(sessionId, "ещё").andExpect(status().isGone());
  }

  @Test
  void finish_from_feedback_stage() throws Exception {
    String sessionId = createSession();
    stubInterviewer(TR_START, A_Q1);
    sendText(sessionId, TR_START).andExpect(status().isOk());

    stubInterviewer(TR_FEEDBACK, A_FEEDBACK_HEADER);
    sendText(sessionId, TR_FEEDBACK).andExpect(status().isOk());

    stubInterviewer(SYS_COMPLETE, SYS_COMPLETE);
    sendText(sessionId, TR_FINISH)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interviewComplete", is(true)));
  }

  // Helpers
  private void stubInterviewer(String userMessage, String assistantAnswer) {
    org.mockito.Mockito.when(interviewerChatClient.prompt().system(anyString())
        .user(org.mockito.Mockito.eq(userMessage))
        .advisors((java.util.function.Consumer<org.springframework.ai.chat.client.ChatClient.AdvisorSpec>) any(java.util.function.Consumer.class))
        .call().content())
        .thenReturn(assistantAnswer);
  }

  private String createSession() throws Exception {
    CreateSessionRequestDto request = CreateSessionRequestDto.builder()
        .mode(SessionMode.VACANCY)
        .vacancyText("Java Developer")
        .numQuestions(3)
        .build();

    String content = mockMvc.perform(post("/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    JsonNode node = objectMapper.readTree(content);
    return node.get("sessionId").asText();
  }

  private ResultActions sendText(String sessionId, String text) throws Exception {
    MessageRequestDto body = MessageRequestDto.builder()
        .type("text")
        .message(text)
        .build();
    return mockMvc.perform(post("/sessions/" + sessionId + "/messages")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body)));
  }

  @Test
  void create_returns_planned_session_with_fields() throws Exception {
    CreateSessionRequestDto request = CreateSessionRequestDto.builder()
        .mode(SessionMode.VACANCY)
        .vacancyText("QA Engineer")
        .numQuestions(2)
        .build();

    String content = mockMvc.perform(post("/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("planned")))
        .andReturn().getResponse().getContentAsString();

    SessionResponseDto resp = objectMapper.readValue(content, SessionResponseDto.class);
    org.assertj.core.api.Assertions.assertThat(resp.getSessionId()).isNotBlank();
    org.assertj.core.api.Assertions.assertThat(resp.getMessages()).isEmpty();
  }

  @Test
  void get_status_and_history_endpoints_work() throws Exception {
    String sessionId = createSession();
    sendText(sessionId, TR_PLAN).andExpect(status().isOk());

    String st = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/sessions/" + sessionId + "/status"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    SessionStatusResponseDto statusDto = objectMapper.readValue(st, SessionStatusResponseDto.class);
    org.assertj.core.api.Assertions.assertThat(statusDto.getStatus()).isEqualTo("planned");

    String hist = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/sessions/" + sessionId))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    SessionResponseDto histDto = objectMapper.readValue(hist, SessionResponseDto.class);
    org.assertj.core.api.Assertions.assertThat(histDto.getSessionId()).isEqualTo(sessionId);
  }

  @Test
  void create_form_with_file_uses_extracted_text() throws Exception {
    MockMultipartFile file = new MockMultipartFile("vacancyFile", "job.txt", "text/plain", "stub".getBytes());
    MockMultipartFile mode = new MockMultipartFile("mode", "mode", "text/plain", "vacancy".getBytes());

    String content = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/sessions/form")
            .file(file)
            .file(mode)
            .param("vacancyUrl", "")
            .param("vacancyText", "")
            .param("roleName", "")
            .param("numQuestions", "3"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    SessionResponseDto resp = objectMapper.readValue(content, SessionResponseDto.class);
    org.assertj.core.api.Assertions.assertThat(resp.getSessionId()).isNotBlank();
  }

  @Test
  void sse_stream_returns_ok() throws Exception {
    String sessionId = createSession();

    org.mockito.Mockito.when(interviewerChatClient.prompt().system(anyString())
        .user(org.mockito.Mockito.eq(TR_START))
        .advisors((java.util.function.Consumer<org.springframework.ai.chat.client.ChatClient.AdvisorSpec>) any(java.util.function.Consumer.class))
        .stream().chatResponse())
        .thenReturn(reactor.core.publisher.Flux.empty());

    MessageRequestDto body = MessageRequestDto.builder().type("text").message(TR_START).build();

    mockMvc.perform(post("/sessions/" + sessionId + "/messages/stream")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isOk());
  }

  @Test
  void roles_suggest_returns_results() throws Exception {
    Role role = Role.builder().name("Java Developer").normalized("java developer").popularity(100).build();
    roleRepository.save(role);
    roleSynonymRepository.save(RoleSynonym.builder().role(role).name("Java dev").normalized("java dev").build());

    String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/roles/suggest").param("q", "java"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    java.util.List<?> arr = objectMapper.readValue(body, java.util.List.class);
    org.assertj.core.api.Assertions.assertThat(arr).isNotEmpty();
  }
}


