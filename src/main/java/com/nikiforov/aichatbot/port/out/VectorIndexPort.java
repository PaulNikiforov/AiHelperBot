package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;

import java.util.List;

public interface VectorIndexPort {

    void index(List<DocumentChunk> chunks);

    void clear();
}
