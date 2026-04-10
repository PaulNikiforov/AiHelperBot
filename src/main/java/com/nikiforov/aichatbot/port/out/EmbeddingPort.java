package com.nikiforov.aichatbot.port.out;

import java.util.List;

public interface EmbeddingPort {

    List<float[]> embed(List<String> texts);

    int dimensions();

    boolean isAvailable();
}
