package com.nikiforov.aichatbot.domain.validation;

import com.nikiforov.aichatbot.domain.model.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidationChainTest {

    @Test
    void allValidatorsPass_returnsPass() {
        InputValidationChain chain = new InputValidationChain(List.of(
                input -> ValidationResult.pass(),
                input -> ValidationResult.pass()
        ));

        ValidationResult result = chain.validate("hello");

        assertThat(result.passed()).isTrue();
    }

    @Test
    void firstValidatorFails_shortCircuits() {
        InputValidationChain chain = new InputValidationChain(List.of(
                input -> ValidationResult.fail("too short"),
                input -> { throw new AssertionError("should not be called"); }
        ));

        ValidationResult result = chain.validate("hi");

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).isEqualTo("too short");
    }

    @Test
    void secondValidatorFails_firstPasses() {
        InputValidationChain chain = new InputValidationChain(List.of(
                input -> ValidationResult.pass(),
                input -> ValidationResult.fail("not English")
        ));

        ValidationResult result = chain.validate("bonjour");

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).isEqualTo("not English");
    }

    @Test
    void emptyValidatorList_returnsPass() {
        InputValidationChain chain = new InputValidationChain(List.of());

        ValidationResult result = chain.validate("anything");

        assertThat(result.passed()).isTrue();
    }
}
