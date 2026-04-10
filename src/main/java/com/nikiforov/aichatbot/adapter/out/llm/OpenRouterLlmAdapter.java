package com.nikiforov.aichatbot.adapter.out.llm;

import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.LlmResponse;
import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.service.rag.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenRouterLlmAdapter implements LlmPort {

    private final LlmClient llmClient;

    @Override
    public String id() {
        return "openrouter";
    }

    @Override
    public LlmResponse ask(String systemMessage, String userMessage) {
        try {
            String answer = llmClient.ask(systemMessage, userMessage);
            return new LlmResponse(answer, 0, 0, id());
        } catch (Exception e) {
            throw new LlmUnavailableException("OpenRouter LLM unavailable: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
