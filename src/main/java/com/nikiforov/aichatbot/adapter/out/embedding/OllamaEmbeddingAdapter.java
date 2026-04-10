package com.nikiforov.aichatbot.adapter.out.embedding;

import com.nikiforov.aichatbot.port.out.EmbeddingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OllamaEmbeddingAdapter implements EmbeddingPort {

    private final EmbeddingModel embeddingModel;

    @Override
    public List<float[]> embed(List<String> texts) {
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(texts, null));
        return response.getResults().stream()
                .map(result -> result.getOutput())
                .toList();
    }

    @Override
    public int dimensions() {
        return 768;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
