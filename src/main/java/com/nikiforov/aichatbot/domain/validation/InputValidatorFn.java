package com.nikiforov.aichatbot.domain.validation;

import com.nikiforov.aichatbot.domain.model.ValidationResult;

@FunctionalInterface
public interface InputValidatorFn {
    ValidationResult validate(String input);
}
