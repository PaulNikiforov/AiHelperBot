package com.nikiforov.aichatbot.controller;

import com.nikiforov.aichatbot.dto.response.BotTopicsResponse;
import com.nikiforov.aichatbot.service.BotTopicsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bot intro")
@RestController
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "false", matchIfMissing = true)
public class BotTopicsController {

    private final BotTopicsService botTopicsService;

    @Operation(summary = "Get bot intro text")
    @GetMapping("/topics")
    public ResponseEntity<BotTopicsResponse> getIntroAndTopics() {
        return ResponseEntity.ok(botTopicsService.getIntroAndTopics());
    }
}
