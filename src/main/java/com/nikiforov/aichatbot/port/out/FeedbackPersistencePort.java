package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;

import java.util.Optional;

public interface FeedbackPersistencePort {

    Feedback save(Feedback feedback);

    Optional<Feedback> findById(FeedbackId id);
}
