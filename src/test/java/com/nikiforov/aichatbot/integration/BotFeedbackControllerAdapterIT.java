package com.nikiforov.aichatbot.integration;

import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackResponse;
import com.nikiforov.aichatbot.config.TestSecurityConfig;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.adapter.out.storage.AzureBlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.ollama.embedding.enabled=false",
        "rag.storage.blob.url=",
        "app.adapters.inbound.enabled=true"
})
class BotFeedbackControllerAdapterIT {

    private static final int MAX_ANSWER_LENGTH_PLUS_ONE = 10_001;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withReuse(true);

    @MockBean
    EmbeddingModel embeddingModel;

    @MockBean
    AzureBlobStorageService azureBlobStorageService;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void saveFeedback_validRequest_returns201WithBody() {
        var request = new FeedbackRequest(
                "What is a performance review?",
                "A performance review is...",
                FeedbackType.LIKE
        );

        ResponseEntity<FeedbackResponse> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, FeedbackResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull().isPositive();
        assertThat(response.getBody().question()).isEqualTo("What is a performance review?");
        assertThat(response.getBody().botFeedbackType()).isEqualTo(FeedbackType.LIKE);
    }

    @Test
    void getFeedback_existingId_returns200() {
        var request = new FeedbackRequest(
                "How do I set goals?",
                "You set goals by...",
                FeedbackType.DISLIKE
        );
        ResponseEntity<FeedbackResponse> saved =
                restTemplate.postForEntity("/api/v1/botfeedback", request, FeedbackResponse.class);
        Long id = saved.getBody().id();

        ResponseEntity<FeedbackResponse> response =
                restTemplate.getForEntity("/api/v1/botfeedback/" + id, FeedbackResponse.class);

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
        assertThat(response.getBody()).contains("errorCode");
        assertThat(response.getBody()).contains("300000");
    }

    @Test
    void saveFeedback_blankQuestion_returns400() {
        var request = new FeedbackRequest("", "Some answer", FeedbackType.LIKE);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
        assertThat(response.getBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void saveFeedback_nullFeedbackType_returns400() {
        var request = new FeedbackRequest("What is PM?", "PM is...", null);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
        assertThat(response.getBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void saveFeedback_answerTooLong_returns400() {
        String tooLong = "x".repeat(MAX_ANSWER_LENGTH_PLUS_ONE);
        var request = new FeedbackRequest("Question?", tooLong, FeedbackType.LIKE);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/botfeedback", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
