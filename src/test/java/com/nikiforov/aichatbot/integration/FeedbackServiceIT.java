package com.nikiforov.aichatbot.integration;

import com.nikiforov.aichatbot.adapter.out.persistence.mapper.FeedbackPersistenceMapper;
import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.port.in.GetFeedbackUseCase;
import com.nikiforov.aichatbot.port.in.SaveFeedbackUseCase;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.adapter.out.persistence.entity.BotFeedback;
import com.nikiforov.aichatbot.adapter.out.persistence.BotFeedbackRepository;
import com.nikiforov.aichatbot.adapter.out.storage.AzureBlobStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.ai.ollama.embedding.enabled=false",
        "rag.storage.blob.url="
})
class FeedbackServiceIT {

    private static final int MAX_ANSWER_LENGTH = 10_000;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withReuse(true);

    @MockBean
    EmbeddingModel embeddingModel;

    @MockBean
    AzureBlobStorageService azureBlobStorageService;

    @Autowired
    SaveFeedbackUseCase saveFeedbackUseCase;

    @Autowired
    GetFeedbackUseCase getFeedbackUseCase;

    @Autowired
    BotFeedbackRepository botFeedbackRepository;

    @Autowired
    FeedbackPersistenceMapper feedbackPersistenceMapper;

    @Test
    void save_answerExactlyAtLimit_savedWithoutTruncation() {
        String answer = "x".repeat(MAX_ANSWER_LENGTH);

        Feedback response = saveFeedbackUseCase.save("What is PM?", answer, FeedbackType.LIKE, "user@test.local");

        BotFeedback saved = botFeedbackRepository.findById(response.getId().value()).orElseThrow();
        assertThat(saved.getAnswer()).hasSize(MAX_ANSWER_LENGTH);
    }

    @Test
    void save_answerLongerThanLimit_isTruncatedTo10000() {
        String longAnswer = "x".repeat(15_000);

        Feedback response = saveFeedbackUseCase.save("What is PM?", longAnswer, FeedbackType.LIKE, "user@test.local");

        BotFeedback saved = botFeedbackRepository.findById(response.getId().value()).orElseThrow();
        assertThat(saved.getAnswer()).hasSize(MAX_ANSWER_LENGTH);
    }

    @Test
    void save_shortAnswer_savedAsIs() {
        String answer = "Short answer.";

        Feedback response = saveFeedbackUseCase.save("Question?", answer, FeedbackType.DISLIKE, "user@test.local");

        BotFeedback saved = botFeedbackRepository.findById(response.getId().value()).orElseThrow();
        assertThat(saved.getAnswer()).isEqualTo(answer);
    }

    @Test
    void getById_existingId_returnsDomainFeedback() {
        Feedback saved = saveFeedbackUseCase.save("Test question?", "Test answer", FeedbackType.LIKE, "test@test.local");
        FeedbackId id = saved.getId();

        Feedback retrieved = getFeedbackUseCase.getById(id);

        assertThat(retrieved.getId()).isEqualTo(id);
        assertThat(retrieved.getQuestion()).isEqualTo("Test question?");
        assertThat(retrieved.getAnswer()).isEqualTo("Test answer");
        assertThat(retrieved.getType()).isEqualTo(FeedbackType.LIKE);
        assertThat(retrieved.getEmployeeEmail()).isEqualTo("test@test.local");
        assertThat(retrieved.getCreatedAt()).isNotNull();
    }

    @Test
    void getById_nonExistentId_throwsFeedbackNotFoundException() {
        FeedbackId nonExistentId = new FeedbackId(99999L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> getFeedbackUseCase.getById(nonExistentId))
                .isInstanceOf(com.nikiforov.aichatbot.domain.exception.FeedbackNotFoundException.class)
                .hasMessageContaining("99999");
    }

    @Test
    void save_withNullEmployeeEmail_savesSuccessfully() {
        Feedback response = saveFeedbackUseCase.save("Question?", "Answer", FeedbackType.LIKE, null);

        BotFeedback saved = botFeedbackRepository.findById(response.getId().value()).orElseThrow();
        assertThat(saved.getEmployeeEmail()).isNull();
    }
}
