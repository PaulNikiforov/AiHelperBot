package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.LlmResponse;

public interface LlmPort {

    String id();

    LlmResponse ask(String systemMessage, String userMessage);

    boolean isAvailable();
}
