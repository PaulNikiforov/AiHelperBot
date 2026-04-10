package com.nikiforov.aichatbot.domain.model;

public record ValidationResult(boolean passed, String reason) {

    public ValidationResult {
        if (passed && reason != null) {
            throw new IllegalArgumentException("A passing result must not carry a reason");
        }
        if (!passed && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A failing result must have a non-blank reason");
        }
    }

    public static ValidationResult pass() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String reason) {
        return new ValidationResult(false, reason);
    }
}
