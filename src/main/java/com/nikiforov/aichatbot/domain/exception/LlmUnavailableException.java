package com.nikiforov.aichatbot.domain.exception;

public class LlmUnavailableException extends DomainException {

    public LlmUnavailableException(String message) {
        super(message);
    }
}
