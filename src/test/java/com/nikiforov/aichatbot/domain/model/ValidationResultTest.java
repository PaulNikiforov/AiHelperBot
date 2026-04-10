package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationResultTest {

    @Test
    void pass_returnsPassedTrueAndReasonNull() {
        ValidationResult result = ValidationResult.pass();
        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    void fail_returnsPassedFalseAndReasonSet() {
        ValidationResult result = ValidationResult.fail("not English");
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).isEqualTo("not English");
    }

    @Test
    void fail_nullReason_throws() {
        assertThatThrownBy(() -> ValidationResult.fail(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fail_blankReason_throws() {
        assertThatThrownBy(() -> ValidationResult.fail("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
