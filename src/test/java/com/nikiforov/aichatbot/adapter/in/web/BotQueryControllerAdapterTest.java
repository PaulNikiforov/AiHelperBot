package com.nikiforov.aichatbot.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikiforov.aichatbot.adapter.in.web.dto.AskRequest;
import com.nikiforov.aichatbot.adapter.in.web.mapper.QuestionWebMapper;
import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.Answer;
import com.nikiforov.aichatbot.domain.model.Question;
import com.nikiforov.aichatbot.port.in.AskQuestionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BotQueryControllerAdapter.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@TestPropertySource(properties = "app.adapters.inbound.enabled=true")
class BotQueryControllerAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AskQuestionUseCase askQuestionUseCase;

    @MockBean
    private QuestionWebMapper questionWebMapper;

    @Test
    void ask_validQuestion_returns200WithAnswer() throws Exception {
        when(questionWebMapper.toQuestion(any(AskRequest.class)))
                .thenReturn(new Question("How do I schedule a review?"));
        when(askQuestionUseCase.ask(any(Question.class)))
                .thenReturn(new Answer("To schedule a review...", 10, 50));
        when(questionWebMapper.toAskResponse(any(Answer.class)))
                .thenReturn(new com.nikiforov.aichatbot.adapter.in.web.dto.AskResponse("To schedule a review..."));

        mockMvc.perform(post("/api/v1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("How do I schedule a review?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("To schedule a review..."));
    }

    @Test
    void ask_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void ask_nullQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void ask_llmUnavailable_returns503() throws Exception {
        when(questionWebMapper.toQuestion(any(AskRequest.class)))
                .thenReturn(new Question("What is performance review?"));
        when(askQuestionUseCase.ask(any(Question.class)))
                .thenThrow(new LlmUnavailableException("All providers failed"));

        mockMvc.perform(post("/api/v1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("What is performance review?"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("LLM_ERROR"));
    }
}
