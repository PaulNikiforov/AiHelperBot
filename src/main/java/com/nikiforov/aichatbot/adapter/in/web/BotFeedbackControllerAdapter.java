package com.nikiforov.aichatbot.adapter.in.web;

import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackResponse;
import com.nikiforov.aichatbot.adapter.in.web.mapper.FeedbackWebMapper;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.port.in.GetFeedbackUseCase;
import com.nikiforov.aichatbot.port.in.SaveFeedbackUseCase;
import com.nikiforov.aichatbot.port.out.IdentityProviderPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/botfeedback")
@Tag(name = "Feedback")
@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "true")
public class BotFeedbackControllerAdapter {

    private final SaveFeedbackUseCase saveFeedbackUseCase;
    private final GetFeedbackUseCase getFeedbackUseCase;
    private final FeedbackWebMapper feedbackWebMapper;
    private final IdentityProviderPort identityProviderPort;

    @Operation(summary = "Save bot feedback")
    @ApiResponse(responseCode = "201", description = "Bot feedback saved")
    @PostMapping
    public ResponseEntity<FeedbackResponse> saveBotFeedback(@Valid @RequestBody FeedbackRequest request) {
        String employeeEmail = identityProviderPort.getCurrentUserEmail();
        var feedback = saveFeedbackUseCase.save(
                request.question(), request.answer(), request.botFeedbackType(), employeeEmail
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackWebMapper.toResponse(feedback));
    }

    @Operation(summary = "Get bot feedback by id")
    @ApiResponse(responseCode = "200", description = "Bot feedback found")
    @ApiResponse(responseCode = "404", description = "Bot feedback not found")
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getBotFeedbackById(@PathVariable Long id) {
        var feedback = getFeedbackUseCase.getById(new FeedbackId(id));
        return ResponseEntity.ok(feedbackWebMapper.toResponse(feedback));
    }
}
