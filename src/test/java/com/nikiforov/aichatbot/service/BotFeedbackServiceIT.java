package com.nikiforov.aichatbot.service;

import com.nikiforov.aichatbot.dto.request.BotFeedbackRequest;
import com.nikiforov.aichatbot.dto.response.BotFeedbackResponse;
import com.nikiforov.aichatbot.model.BotFeedback;
import com.nikiforov.aichatbot.model.BotFeedbackType;
import com.nikiforov.aichatbot.repository.BotFeedbackRepository;
import com.nikiforov.aichatbot.service.rag.AzureBlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.ollama.embedding.enabled=false",
        "rag.storage.blob.url="
})
class BotFeedbackServiceIT {

    private static final int MAX_ANSWER_LENGTH = 10_000;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @MockBean
    EmbeddingModel embeddingModel;

    @MockBean
    AzureBlobStorageService azureBlobStorageService;

    @Autowired
    BotFeedbackService botFeedbackService;

    @Autowired
    BotFeedbackRepository botFeedbackRepository;

    @Test
    void save_answerExactlyAtLimit_savedWithoutTruncation() {
        String answer = "x".repeat(MAX_ANSWER_LENGTH);
        var request = new BotFeedbackRequest("What is PM?", answer, BotFeedbackType.LIKE);

        BotFeedbackResponse response = botFeedbackService.save(request);

        BotFeedback saved = botFeedbackRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getAnswer()).hasSize(MAX_ANSWER_LENGTH);
    }

    @Test
    void save_answerLongerThanLimit_isTruncatedTo10000() {
        String longAnswer = "x".repeat(15_000);
        var request = new BotFeedbackRequest("What is PM?", longAnswer, BotFeedbackType.LIKE);

        BotFeedbackResponse response = botFeedbackService.save(request);

        BotFeedback saved = botFeedbackRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getAnswer()).hasSize(MAX_ANSWER_LENGTH);
    }

    @Test
    void save_shortAnswer_savedAsIs() {
        String answer = "Short answer.";
        var request = new BotFeedbackRequest("Question?", answer, BotFeedbackType.DISLIKE);

        BotFeedbackResponse response = botFeedbackService.save(request);

        BotFeedback saved = botFeedbackRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getAnswer()).isEqualTo(answer);
    }
}
