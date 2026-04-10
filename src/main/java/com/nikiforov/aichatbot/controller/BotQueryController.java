package com.nikiforov.aichatbot.controller;

import com.nikiforov.aichatbot.dto.request.LlmRequest;
import com.nikiforov.aichatbot.dto.response.LlmResponse;
import com.nikiforov.aichatbot.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Question for LLM")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "false", matchIfMissing = true)
public class BotQueryController {

    private final RagService ragService;

    @Operation(summary = "Ask LLM")
    @PostMapping("/ask")
    public ResponseEntity<LlmResponse> ask(@Valid @RequestBody LlmRequest request) {
        String answer = ragService.answer(request.question());
        return ResponseEntity.ok(new LlmResponse(answer));
    }
}
