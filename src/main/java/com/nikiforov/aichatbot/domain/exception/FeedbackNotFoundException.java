package com.nikiforov.aichatbot.domain.exception;

import com.nikiforov.aichatbot.domain.model.FeedbackId;

public class FeedbackNotFoundException extends DomainException {

    public FeedbackNotFoundException(FeedbackId id) {
        super("Feedback not found with id: " + id.value());
    }
}
