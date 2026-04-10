package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptAssemblerTest {

    private final PromptAssembler assembler = new PromptAssembler();

    private DocumentChunk chunk(String content, int page) {
        return new DocumentChunk("id", content, page, "guide.pdf", Map.of());
    }

    @Test
    void buildsUserMessageWithPageLabeledContext() {
        List<DocumentChunk> docs = List.of(
                chunk("content about reviews", 5),
                chunk("content about deadlines", 10)
        );

        PromptAssembler.ChatMessages messages = assembler.build("How do reviews work?", docs);

        assertThat(messages.systemMessage()).containsIgnoringCase("answer");
        assertThat(messages.userMessage()).contains("How do reviews work?");
        assertThat(messages.userMessage()).contains("Page 5");
        assertThat(messages.userMessage()).contains("Page 10");
        assertThat(messages.userMessage()).contains("content about reviews");
        assertThat(messages.userMessage()).contains("content about deadlines");
    }

    @Test
    void handlesEmptyContext() {
        PromptAssembler.ChatMessages messages = assembler.build("What is X?", List.of());

        assertThat(messages.systemMessage()).isNotBlank();
        assertThat(messages.userMessage()).contains("What is X?");
    }
}
