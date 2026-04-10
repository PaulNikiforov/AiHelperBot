package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerTest {

    @Test
    void defaultResponse_returnsExpectedTextAndZeroTokens() {
        Answer answer = Answer.defaultResponse();
        assertThat(answer.text()).isEqualTo("Not specified in the User Guide");
        assertThat(answer.promptTokens()).isZero();
        assertThat(answer.completionTokens()).isZero();
    }

    @Test
    void unavailable_returnsExpectedTextAndZeroTokens() {
        Answer answer = Answer.unavailable();
        assertThat(answer.text()).isEqualTo("AI Helper Bot temporarily unavailable. Check application logs for details.");
        assertThat(answer.promptTokens()).isZero();
        assertThat(answer.completionTokens()).isZero();
    }

    @Test
    void storesTextAndTokenCounts() {
        Answer answer = new Answer("Some text", 100, 50);
        assertThat(answer.text()).isEqualTo("Some text");
        assertThat(answer.promptTokens()).isEqualTo(100);
        assertThat(answer.completionTokens()).isEqualTo(50);
    }
}
