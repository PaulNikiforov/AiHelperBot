package com.nikiforov.aichatbot.domain.model;

import java.util.Map;

public record DocumentChunk(String id, String content, int pageNumber, String source, Map<String, Object> metadata) {
}
