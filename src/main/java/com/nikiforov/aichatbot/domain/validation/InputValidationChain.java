package com.nikiforov.aichatbot.domain.validation;

import com.nikiforov.aichatbot.domain.model.ValidationResult;

import java.util.List;

public class InputValidationChain {

    private final List<InputValidatorFn> validators;

    public InputValidationChain(List<InputValidatorFn> validators) {
        this.validators = validators;
    }

    public ValidationResult validate(String input) {
        for (InputValidatorFn validator : validators) {
            ValidationResult result = validator.validate(input);
            if (!result.passed()) {
                return result;
            }
        }
        return ValidationResult.pass();
    }
}
