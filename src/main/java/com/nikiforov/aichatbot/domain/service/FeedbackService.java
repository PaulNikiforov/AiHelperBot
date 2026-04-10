package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.exception.FeedbackNotFoundException;
import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.port.in.GetFeedbackUseCase;
import com.nikiforov.aichatbot.port.in.SaveFeedbackUseCase;
import com.nikiforov.aichatbot.port.out.FeedbackPersistencePort;

public class FeedbackService implements SaveFeedbackUseCase, GetFeedbackUseCase {

    private final FeedbackPersistencePort persistence;

    public FeedbackService(FeedbackPersistencePort persistence) {
        this.persistence = persistence;
    }

    @Override
    public Feedback save(String question, String answer, FeedbackType type, String employeeEmail) {
        FeedbackId id = null;
        Feedback feedback = new Feedback(id, question, answer, type, employeeEmail, null);
        return persistence.save(feedback);
    }

    @Override
    public Feedback getById(FeedbackId id) {
        return persistence.findById(id)
                .orElseThrow(() -> new FeedbackNotFoundException(id));
    }
}
