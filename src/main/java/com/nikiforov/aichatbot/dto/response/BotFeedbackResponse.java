package com.nikiforov.aichatbot.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nikiforov.aichatbot.model.BotFeedbackType;

import java.time.LocalDateTime;

/**
 * DTO for returning saved bot feedback to the caller.
 */
public record BotFeedbackResponse(
        Long id,
        String question,
        String answer,
        BotFeedbackType botFeedbackType,
        String employeeEmail,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime createdAt
) {}
