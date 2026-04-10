package com.nikiforov.aichatbot.domain.model;

import java.time.Instant;

public class Feedback {

    private static final int MAX_ANSWER_LENGTH = 10_000;

    private final FeedbackId id;
    private final String question;
    private final String answer;
    private final FeedbackType type;
    private final String employeeEmail;
    private final Instant createdAt;

    public Feedback(FeedbackId id, String question, String answer, FeedbackType type, String employeeEmail, Instant createdAt) {
        this.id = id;
        this.question = question;
        this.answer = truncate(answer);
        this.type = type;
        this.employeeEmail = employeeEmail;
        this.createdAt = createdAt;
    }

    public FeedbackId getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public FeedbackType getType() {
        return type;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String truncate(String value) {
        if (value != null && value.length() > MAX_ANSWER_LENGTH) {
            return value.substring(0, MAX_ANSWER_LENGTH);
        }
        return value;
    }
}
