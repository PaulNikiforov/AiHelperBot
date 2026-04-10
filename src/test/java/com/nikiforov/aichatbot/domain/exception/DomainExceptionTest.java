package com.nikiforov.aichatbot.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void constructorWithMessage() {
        DomainException ex = new DomainException("something went wrong");
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        DomainException ex = new DomainException("wrapped error", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped error");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
