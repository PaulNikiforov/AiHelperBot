package com.nikiforov.aichatbot.service;

import com.nikiforov.aichatbot.config.properties.BotProperties;
import com.nikiforov.aichatbot.dto.response.BotTopicsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotTopicsServiceImpl implements BotTopicsService {

    private final BotProperties botProperties;

    @Override
    public BotTopicsResponse getIntroAndTopics() {
        log.info("Returning intro text");
        return new BotTopicsResponse(botProperties.getIntroText());
    }
}
