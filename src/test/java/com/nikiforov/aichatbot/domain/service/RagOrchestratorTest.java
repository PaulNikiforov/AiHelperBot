package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.*;
import com.nikiforov.aichatbot.domain.validation.InputValidationChain;
import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.port.out.VectorSearchPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagOrchestratorTest {

    private final StubVectorSearch vectorSearch = new StubVectorSearch();
    private final StubLlmPort llmPort = new StubLlmPort();
    private InputValidationChain passingChain = new InputValidationChain(List.of(
            input -> ValidationResult.pass()
    ));
    private InputValidationChain failingChain = new InputValidationChain(List.of(
            input -> ValidationResult.fail("rejected")
    ));

    private DocumentChunk chunk(String content) {
        return new DocumentChunk("id", content, 1, "guide.pdf", Map.of());
    }

    @Test
    void vectorStoreNotLoaded_returnsUnavailable() {
        vectorSearch.loaded = false;
        RagOrchestrator orchestrator = new RagOrchestrator(passingChain, new QueryClassifier(),
                vectorSearch, new DocumentRanker(), new PromptAssembler(), llmPort);

        Answer answer = orchestrator.ask(new Question("What is X?"));

        assertThat(answer).isEqualTo(Answer.unavailable());
    }

    @Test
    void inputValidationFails_returnsRejectionMessage() {
        RagOrchestrator orchestrator = new RagOrchestrator(failingChain, new QueryClassifier(),
                vectorSearch, new DocumentRanker(), new PromptAssembler(), llmPort);

        Answer answer = orchestrator.ask(new Question("hi"));

        assertThat(answer.text()).contains("rejected");
    }

    @Test
    void noDocumentsFound_returnsDefaultResponse() {
        vectorSearch.results = List.of();
        RagOrchestrator orchestrator = new RagOrchestrator(passingChain, new QueryClassifier(),
                vectorSearch, new DocumentRanker(), new PromptAssembler(), llmPort);

        Answer answer = orchestrator.ask(new Question("What is X?"));

        assertThat(answer).isEqualTo(Answer.defaultResponse());
    }

    @Test
    void happyPath_returnsLlmAnswer() {
        vectorSearch.results = List.of(chunk("some relevant content"));
        llmPort.response = new LlmResponse("X is a review process", 100, 50, "test-provider");

        RagOrchestrator orchestrator = new RagOrchestrator(passingChain, new QueryClassifier(),
                vectorSearch, new DocumentRanker(), new PromptAssembler(), llmPort);

        Answer answer = orchestrator.ask(new Question("What is X?"));

        assertThat(answer.text()).isEqualTo("X is a review process");
        assertThat(answer.promptTokens()).isEqualTo(100);
        assertThat(answer.completionTokens()).isEqualTo(50);
    }

    @Test
    void llmFailure_throwsException() {
        vectorSearch.results = List.of(chunk("content"));
        llmPort.shouldThrow = true;

        RagOrchestrator orchestrator = new RagOrchestrator(passingChain, new QueryClassifier(),
                vectorSearch, new DocumentRanker(), new PromptAssembler(), llmPort);

        assertThatThrownBy(() -> orchestrator.ask(new Question("What is X?")))
                .isInstanceOf(LlmUnavailableException.class);
    }

    private static class StubVectorSearch implements VectorSearchPort {
        boolean loaded = true;
        List<DocumentChunk> results = List.of();

        @Override
        public List<DocumentChunk> search(String query, int topK) {
            return results;
        }

        @Override
        public List<DocumentChunk> findByPageNumber(int pageNumber) {
            return results;
        }

        @Override
        public boolean isLoaded() {
            return loaded;
        }
    }

    private static class StubLlmPort implements LlmPort {
        LlmResponse response = new LlmResponse("default answer", 10, 5, "stub");
        boolean shouldThrow = false;

        @Override
        public String id() { return "stub"; }

        @Override
        public LlmResponse ask(String systemMessage, String userMessage) {
            if (shouldThrow) {
                throw new LlmUnavailableException("LLM unavailable");
            }
            return response;
        }

        @Override
        public boolean isAvailable() { return !shouldThrow; }
    }
}
