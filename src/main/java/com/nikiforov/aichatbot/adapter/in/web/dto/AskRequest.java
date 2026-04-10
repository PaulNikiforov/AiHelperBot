package com.nikiforov.aichatbot.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank(message = "Question must not be blank")
        @Size(max = 4000, message = "Question must be at most 4000 characters")
        String question
) {}
