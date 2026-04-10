package com.nikiforov.aichatbot.port.in;

import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackType;

public interface SaveFeedbackUseCase {

    Feedback save(String question, String answer, FeedbackType type, String employeeEmail);
}
