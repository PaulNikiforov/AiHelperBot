package com.nikiforov.aichatbot.adapter.in.web;

import com.nikiforov.aichatbot.adapter.in.web.dto.AskRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.AskResponse;
import com.nikiforov.aichatbot.adapter.in.web.mapper.QuestionWebMapper;
import com.nikiforov.aichatbot.domain.model.Question;
import com.nikiforov.aichatbot.port.in.AskQuestionUseCase;
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

@Tag(name = "Question")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.adapters.inbound.enabled", havingValue = "true")
public class BotQueryControllerAdapter {

    private final AskQuestionUseCase askQuestionUseCase;
    private final QuestionWebMapper questionWebMapper;

    @Operation(summary = "Ask LLM")
    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        Question question = questionWebMapper.toQuestion(request);
        var answer = askQuestionUseCase.ask(question);
        return ResponseEntity.ok(questionWebMapper.toAskResponse(answer));
    }
}
