package com.nikiforov.aichatbot.domain.model;

public record Question(String text) {

    public Question {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Question text must not be blank");
        }
    }
}
