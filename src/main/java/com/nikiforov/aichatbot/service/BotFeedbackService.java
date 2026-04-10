package com.nikiforov.aichatbot.service;

import com.nikiforov.aichatbot.dto.request.BotFeedbackRequest;
import com.nikiforov.aichatbot.dto.response.BotFeedbackResponse;

public interface BotFeedbackService {

    BotFeedbackResponse save(BotFeedbackRequest request);

    BotFeedbackResponse findById(Long id);
}
