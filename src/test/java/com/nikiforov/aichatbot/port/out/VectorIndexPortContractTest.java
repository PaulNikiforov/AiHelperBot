package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class VectorIndexPortContractTest {

    public abstract VectorIndexPort createIndexPort();
    public abstract VectorSearchPort createSearchPort();

    @Test
    void index_thenSearch_returnsIndexedData() {
        VectorIndexPort indexPort = createIndexPort();
        VectorSearchPort searchPort = createSearchPort();

        indexPort.index(List.of(
                new DocumentChunk("c1", "hello world", 1, "guide.pdf", Map.of())
        ));

        List<DocumentChunk> results = searchPort.search("hello", 1);
        assertThat(results).isNotEmpty();
    }

    @Test
    void clear_removesAllData() {
        VectorIndexPort indexPort = createIndexPort();
        VectorSearchPort searchPort = createSearchPort();

        indexPort.index(List.of(
                new DocumentChunk("c1", "hello world", 1, "guide.pdf", Map.of())
        ));
        indexPort.clear();

        boolean loaded = searchPort.isLoaded();
        assertThat(loaded).isFalse();
    }
}
