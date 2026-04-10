package com.nikiforov.aichatbot.adapter.out.llm;

import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.LlmResponse;
import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.adapter.out.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

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
        } catch (RestClientException e) {
            // Specific HTTP/REST errors with context
            throw new LlmUnavailableException("OpenRouter LLM service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            // Generic fallback for unexpected errors
            throw new LlmUnavailableException("OpenRouter LLM unexpected error: " + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
