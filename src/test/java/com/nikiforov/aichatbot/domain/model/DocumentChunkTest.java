package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkTest {

    @Test
    void storesAllFields() {
        Map<String, Object> metadata = Map.of("key", "value");
        DocumentChunk chunk = new DocumentChunk("id1", "content", 5, "guide.pdf", metadata);

        assertThat(chunk.id()).isEqualTo("id1");
        assertThat(chunk.content()).isEqualTo("content");
        assertThat(chunk.pageNumber()).isEqualTo(5);
        assertThat(chunk.source()).isEqualTo("guide.pdf");
        assertThat(chunk.metadata()).containsEntry("key", "value");
    }

    @Test
    void allowsEmptyMetadataMap() {
        DocumentChunk chunk = new DocumentChunk("id2", "content", 1, "guide.pdf", Map.of());
        assertThat(chunk.metadata()).isEmpty();
    }
}
