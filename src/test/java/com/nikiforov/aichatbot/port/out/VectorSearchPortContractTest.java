package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class VectorSearchPortContractTest {

    public abstract VectorSearchPort createSearchPort();

    @Test
    void search_returnsListOfDocumentChunks() {
        VectorSearchPort port = createSearchPort();

        List<DocumentChunk> results = port.search("test query", 5);

        assertThat(results).isNotNull();
    }

    @Test
    void findByPageNumber_returnsListOfDocumentChunks() {
        VectorSearchPort port = createSearchPort();

        List<DocumentChunk> results = port.findByPageNumber(1);

        assertThat(results).isNotNull();
    }

    @Test
    void isLoaded_returnsBoolean() {
        VectorSearchPort port = createSearchPort();

        boolean loaded = port.isLoaded();

        assertThat(loaded).isTrue();
    }
}
