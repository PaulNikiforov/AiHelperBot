package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseTest {

    @Test
    void storesAllFields() {
        LlmResponse response = new LlmResponse("answer text", 150, 80, "openrouter");
        assertThat(response.answerText()).isEqualTo("answer text");
        assertThat(response.promptTokens()).isEqualTo(150);
        assertThat(response.completionTokens()).isEqualTo(80);
        assertThat(response.providerId()).isEqualTo("openrouter");
    }
}
