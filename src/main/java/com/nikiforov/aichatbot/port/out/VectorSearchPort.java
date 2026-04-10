package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;

import java.util.List;

public interface VectorSearchPort {

    List<DocumentChunk> search(String query, int topK);

    List<DocumentChunk> findByPageNumber(int pageNumber);

    boolean isLoaded();
}
