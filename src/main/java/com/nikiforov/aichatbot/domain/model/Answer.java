package com.nikiforov.aichatbot.domain.model;

public record Answer(String text, int promptTokens, int completionTokens) {

    public static Answer defaultResponse() {
        return new Answer("Not specified in the User Guide", 0, 0);
    }

    public static Answer unavailable() {
        return new Answer("AI Helper Bot temporarily unavailable. Check application logs for details.", 0, 0);
    }
}
