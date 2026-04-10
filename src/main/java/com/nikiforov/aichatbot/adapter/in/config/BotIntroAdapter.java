package com.nikiforov.aichatbot.adapter.in.config;

import com.nikiforov.aichatbot.config.properties.BotProperties;
import com.nikiforov.aichatbot.port.in.GetBotIntroUseCase;
import org.springframework.stereotype.Component;

/**
 * Adapter for providing bot intro text from configuration.
 * Implements {@link GetBotIntroUseCase} by reading from {@link BotProperties}.
 */
@Component
public class BotIntroAdapter implements GetBotIntroUseCase {

    private final BotProperties botProperties;

    public BotIntroAdapter(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    @Override
    public String getIntroText() {
        return botProperties.getIntroText();
    }
}
