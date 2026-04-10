package com.nikiforov.aichatbot.adapter.in.web;

import com.nikiforov.aichatbot.adapter.in.web.dto.IntroResponse;
import com.nikiforov.aichatbot.port.in.GetBotIntroUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bot")
@RestController
@RequestMapping("/api/v1/bot")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "true")
public class BotIntroControllerAdapter {

    private final GetBotIntroUseCase getBotIntroUseCase;

    @Operation(summary = "Get bot intro text")
    @GetMapping("/intro")
    public ResponseEntity<IntroResponse> getIntro() {
        return ResponseEntity.ok(new IntroResponse(getBotIntroUseCase.getIntroText()));
    }
}
