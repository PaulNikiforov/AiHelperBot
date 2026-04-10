package com.nikiforov.aichatbot.integration;

import com.nikiforov.aichatbot.adapter.in.web.dto.AskRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.AskResponse;
import com.nikiforov.aichatbot.adapter.out.storage.AzureBlobStorageService;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.ollama.embedding.enabled=false",
        "rag.storage.blob.url=",
        "app.adapters.inbound.enabled=true",
        "rag.validation.quick-filter.min-length=2"
})
class AskQuestionIT {

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
    void ask_validQuestion_returns200() {
        var request = new AskRequest("What is a performance review?");

        ResponseEntity<AskResponse> response =
                restTemplate.postForEntity("/api/v1/ask", request, AskResponse.class);

        // Vector store is not loaded in this test context, so we expect the unavailable response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // When vector store is not loaded, the answer should contain the unavailable message
        assertThat(response.getBody().answer()).contains("temporarily unavailable");
    }

    @Test
    void ask_blankQuestion_returns400() {
        var request = new AskRequest("");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/ask", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
        assertThat(response.getBody()).contains("VALIDATION_FAILED");
    }

    @Test
    void ask_nullQuestion_returns400() {
        var request = new AskRequest(null);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/ask", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
    }

    @Test
    void ask_shortQuestion_returns400() {
        var request = new AskRequest("x");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/ask", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
    }

    @Test
    void ask_questionWithNoLetters_returns400() {
        var request = new AskRequest("1234567890");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/ask", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
    }

    @Test
    void ask_questionTooLong_returns400() {
        var request = new AskRequest("x".repeat(4001));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/ask", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("errorCode");
    }
}
