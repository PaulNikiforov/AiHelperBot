package com.nikiforov.aichatbot.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nikiforov.aichatbot.domain.model.FeedbackType;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        String question,
        String answer,
        FeedbackType botFeedbackType,
        String employeeEmail,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt
) {}
