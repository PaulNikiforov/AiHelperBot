package com.nikiforov.aichatbot.controller;

import com.nikiforov.aichatbot.dto.request.BotFeedbackRequest;
import com.nikiforov.aichatbot.dto.response.BotFeedbackResponse;
import com.nikiforov.aichatbot.model.BotFeedbackType;
import com.nikiforov.aichatbot.service.rag.AzureBlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.ollama.embedding.enabled=false",
        "rag.storage.blob.url="
})
class BotFeedbackControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @MockBean
    EmbeddingModel embeddingModel;

    @MockBean
    AzureBlobStorageService azureBlobStorageService;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void saveFeedback_validRequest_returns201WithBody() {
        var request = new BotFeedbackRequest("What is a performance review?", "A performance review is...", BotFeedbackType.LIKE);

        ResponseEntity<BotFeedbackResponse> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, BotFeedbackResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull().isPositive();
        assertThat(response.getBody().question()).isEqualTo("What is a performance review?");
        assertThat(response.getBody().botFeedbackType()).isEqualTo(BotFeedbackType.LIKE);
    }

    @Test
    void getFeedback_existingId_returns200() {
        var request = new BotFeedbackRequest("How do I set goals?", "You set goals by...", BotFeedbackType.DISLIKE);
        ResponseEntity<BotFeedbackResponse> saved =
                restTemplate.postForEntity("/api/v1/botfeedback", request, BotFeedbackResponse.class);
        Long id = saved.getBody().id();

        ResponseEntity<BotFeedbackResponse> response =
                restTemplate.getForEntity("/api/v1/botfeedback/" + id, BotFeedbackResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(id);
        assertThat(response.getBody().question()).isEqualTo("How do I set goals?");
    }

    @Test
    void getFeedback_nonExistentId_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/botfeedback/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void saveFeedback_blankQuestion_returns400() {
        var request = new BotFeedbackRequest("", "Some answer", BotFeedbackType.LIKE);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void saveFeedback_nullFeedbackType_returns400() {
        var request = new BotFeedbackRequest("What is PM?", "PM is...", null);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
