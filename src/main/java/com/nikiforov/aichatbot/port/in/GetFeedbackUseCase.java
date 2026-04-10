package com.nikiforov.aichatbot.port.in;

import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;

public interface GetFeedbackUseCase {

    Feedback getById(FeedbackId id);
}
