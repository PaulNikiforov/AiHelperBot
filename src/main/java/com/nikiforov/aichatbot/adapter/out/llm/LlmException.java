package com.nikiforov.aichatbot.adapter.out.llm;

/**
 * Exception thrown when LLM API calls fail.
 * Internal to the LLM adapter - will be caught and converted to domain LlmUnavailableException.
 */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
