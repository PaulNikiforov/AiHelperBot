package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.*;
import com.nikiforov.aichatbot.domain.validation.InputValidationChain;
import com.nikiforov.aichatbot.port.in.AskQuestionUseCase;
import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.port.out.VectorSearchPort;

import java.util.List;

public class RagOrchestrator implements AskQuestionUseCase {

    private final InputValidationChain validationChain;
    private final QueryClassifier queryClassifier;
    private final VectorSearchPort vectorSearch;
    private final DocumentRanker documentRanker;
    private final PromptAssembler promptAssembler;
    private final LlmPort llmPort;

    public RagOrchestrator(InputValidationChain validationChain,
                           QueryClassifier queryClassifier,
                           VectorSearchPort vectorSearch,
                           DocumentRanker documentRanker,
                           PromptAssembler promptAssembler,
                           LlmPort llmPort) {
        this.validationChain = validationChain;
        this.queryClassifier = queryClassifier;
        this.vectorSearch = vectorSearch;
        this.documentRanker = documentRanker;
        this.promptAssembler = promptAssembler;
        this.llmPort = llmPort;
    }

    @Override
    public Answer ask(Question question) {
        if (!vectorSearch.isLoaded()) {
            return Answer.unavailable();
        }

        ValidationResult validation = validationChain.validate(question.text());
        if (!validation.passed()) {
            return new Answer(validation.reason(), 0, 0);
        }

        QueryType queryType = queryClassifier.classify(question.text());
        List<DocumentChunk> candidates = vectorSearch.search(question.text(), 20);
        List<DocumentChunk> ranked = documentRanker.rankByKeywords(candidates, question.text(), 5);

        if (ranked.isEmpty()) {
            return Answer.defaultResponse();
        }

        PromptAssembler.ChatMessages messages = promptAssembler.build(question.text(), ranked);

        LlmResponse llmResponse = llmPort.ask(messages.systemMessage(), messages.userMessage());

        return new Answer(llmResponse.answerText(), llmResponse.promptTokens(), llmResponse.completionTokens());
    }
}
