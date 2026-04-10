package com.nikiforov.aichatbot.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmUnavailableExceptionTest {

    @Test
    void extendsDomainException() {
        LlmUnavailableException ex = new LlmUnavailableException("all providers failed");
        assertThat(ex).isInstanceOf(DomainException.class);
    }

    @Test
    void constructsWitMessage() {
        LlmUnavailableException ex = new LlmUnavailableException("all providers failed");
        assertThat(ex.getMessage()).isEqualTo("all providers failed");
    }
}
