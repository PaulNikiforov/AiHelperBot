package com.nikiforov.aichatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LlmRequest(@NotBlank(message = "Question must not be blank") String question) {}
