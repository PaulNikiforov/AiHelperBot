package com.nikiforov.aichatbot.domain.validation;

import com.nikiforov.aichatbot.domain.model.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatValidatorTest {

    private final FormatValidator validator = new FormatValidator(2);

    @Test
    void rejectsNull() {
        ValidationResult result = validator.validate(null);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).containsIgnoringCase("question");
    }

    @Test
    void rejectsBlank() {
        ValidationResult result = validator.validate("   ");
        assertThat(result.passed()).isFalse();
    }

    @Test
    void rejectsInputShorterThanMinLength() {
        FormatValidator strict = new FormatValidator(5);
        ValidationResult result = strict.validate("hi");
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("5");
    }

    @Test
    void rejectsInputWithNoUnicodeLetters() {
        ValidationResult result = validator.validate("123 456 !!!");
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).containsIgnoringCase("letter");
    }

    @Test
    void acceptsValidInput() {
        ValidationResult result = validator.validate("What is a growth plan?");
        assertThat(result.passed()).isTrue();
    }
}
