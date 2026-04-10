package com.nikiforov.aichatbot.domain.model;

public record LlmResponse(String answerText, int promptTokens, int completionTokens, String providerId) {
}
