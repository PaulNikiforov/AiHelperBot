package com.nikiforov.aichatbot.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackResponse;
import com.nikiforov.aichatbot.adapter.in.web.mapper.FeedbackWebMapper;
import com.nikiforov.aichatbot.domain.exception.FeedbackNotFoundException;
import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.port.in.GetFeedbackUseCase;
import com.nikiforov.aichatbot.port.in.SaveFeedbackUseCase;
import com.nikiforov.aichatbot.port.out.IdentityProviderPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BotFeedbackControllerAdapter.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = "app.adapters.inbound.enabled=true")
class BotFeedbackControllerAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaveFeedbackUseCase saveFeedbackUseCase;

    @MockBean
    private GetFeedbackUseCase getFeedbackUseCase;

    @MockBean
    private FeedbackWebMapper feedbackWebMapper;

    @MockBean
    private IdentityProviderPort identityProviderPort;

    @Test
    void saveFeedback_validRequest_returns201() throws Exception {
        var saved = new Feedback(
                new FeedbackId(1L), "Q", "A", FeedbackType.LIKE, null, Instant.now()
        );
        when(identityProviderPort.getCurrentUserEmail()).thenReturn(null);
        when(saveFeedbackUseCase.save(anyString(), anyString(), any(FeedbackType.class), any()))
                .thenReturn(saved);
        when(feedbackWebMapper.toResponse(any(Feedback.class)))
                .thenReturn(new FeedbackResponse(1L, "Q", "A", FeedbackType.LIKE, null, Instant.now()));

        mockMvc.perform(post("/api/v1/botfeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FeedbackRequest("Q", "A", FeedbackType.LIKE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.botFeedbackType").value("LIKE"));
    }

    @Test
    void saveFeedback_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/botfeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\",\"answer\":\"A\",\"botFeedbackType\":\"LIKE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void saveFeedback_blankAnswer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/botfeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Q\",\"answer\":\"\",\"botFeedbackType\":\"LIKE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void saveFeedback_nullBotFeedbackType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/botfeedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Q\",\"answer\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getFeedback_existingId_returns200() throws Exception {
        var feedback = new Feedback(
                new FeedbackId(1L), "Q", "A", FeedbackType.LIKE, "user@test.com", Instant.now()
        );
        when(getFeedbackUseCase.getById(any())).thenReturn(feedback);
        when(feedbackWebMapper.toResponse(any(Feedback.class)))
                .thenReturn(new FeedbackResponse(1L, "Q", "A", FeedbackType.LIKE, "user@test.com", Instant.now()));

        mockMvc.perform(get("/api/v1/botfeedback/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.question").value("Q"));
    }

    @Test
    void getFeedback_nonExistentId_returns404() throws Exception {
        when(getFeedbackUseCase.getById(any()))
                .thenThrow(new FeedbackNotFoundException(new FeedbackId(99L)));

        mockMvc.perform(get("/api/v1/botfeedback/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOT_FEEDBACK_NOT_FOUND"));
    }
}
