package com.nikiforov.aichatbot.domain.validation;

import com.nikiforov.aichatbot.domain.model.ValidationResult;

public class FormatValidator implements InputValidatorFn {

    private final int minLength;

    public FormatValidator(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.fail("Please enter a question");
        }

        String trimmed = input.trim();

        if (trimmed.length() < minLength) {
            return ValidationResult.fail(
                    "Please enter a more detailed question (at least " + minLength + " characters)");
        }

        if (trimmed.chars().noneMatch(Character::isLetter)) {
            return ValidationResult.fail("Please enter a valid question with letters");
        }

        return ValidationResult.pass();
    }
}
