package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

    @Test
    void constructsAllFields() {
        Instant now = Instant.now();
        Feedback feedback = new Feedback(
                new FeedbackId(1L),
                "What is X?",
                "X is a thing",
                FeedbackType.LIKE,
                "user@example.com",
                now
        );

        assertThat(feedback.getId()).isEqualTo(new FeedbackId(1L));
        assertThat(feedback.getQuestion()).isEqualTo("What is X?");
        assertThat(feedback.getAnswer()).isEqualTo("X is a thing");
        assertThat(feedback.getType()).isEqualTo(FeedbackType.LIKE);
        assertThat(feedback.getEmployeeEmail()).isEqualTo("user@example.com");
        assertThat(feedback.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void truncatesAnswerToMaxLimit() {
        String longAnswer = "a".repeat(10_001);
        Feedback feedback = new Feedback(
                new FeedbackId(1L),
                "Q",
                longAnswer,
                FeedbackType.LIKE,
                null,
                null
        );
        assertThat(feedback.getAnswer()).hasSize(10_000);
    }

    @Test
    void allowsNullEmployeeEmail() {
        Feedback feedback = new Feedback(
                new FeedbackId(1L),
                "Q",
                "A",
                FeedbackType.DISLIKE,
                null,
                null
        );
        assertThat(feedback.getEmployeeEmail()).isNull();
    }

    @Test
    void answerExactlyAtLimitIsNotTruncated() {
        String exactAnswer = "a".repeat(10_000);
        Feedback feedback = new Feedback(
                new FeedbackId(1L),
                "Q",
                exactAnswer,
                FeedbackType.LIKE,
                null,
                null
        );
        assertThat(feedback.getAnswer()).hasSize(10_000);
        assertThat(feedback.getAnswer()).isEqualTo(exactAnswer);
    }
}
