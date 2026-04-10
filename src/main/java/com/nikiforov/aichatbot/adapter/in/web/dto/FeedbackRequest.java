package com.nikiforov.aichatbot.adapter.in.web.dto;

import com.nikiforov.aichatbot.domain.model.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 4000, message = "Question too long")
        String question,

        @NotBlank(message = "Answer required")
        @Size(max = 10000, message = "Answer too long")
        String answer,

        @NotNull
        FeedbackType botFeedbackType
) {}
