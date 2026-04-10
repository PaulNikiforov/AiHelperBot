package com.nikiforov.aichatbot.adapter.out.vectorstore;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;
import com.nikiforov.aichatbot.port.out.VectorIndexPort;
import com.nikiforov.aichatbot.port.out.VectorSearchPort;
import com.nikiforov.aichatbot.service.rag.EmbeddingIndexer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InMemoryVectorStoreAdapter implements VectorSearchPort, VectorIndexPort {

    private final EmbeddingIndexer embeddingIndexer;

    @Override
    public List<DocumentChunk> search(String query, int topK) {
        List<Document> docs = embeddingIndexer.search(query, topK);
        return docs.stream().map(this::toChunk).toList();
    }

    @Override
    public List<DocumentChunk> findByPageNumber(int pageNumber) {
        List<Document> docs = embeddingIndexer.findByPageNumber(pageNumber);
        return docs.stream().map(this::toChunk).toList();
    }

    @Override
    public boolean isLoaded() {
        return embeddingIndexer.isLoaded();
    }

    @Override
    public void index(List<DocumentChunk> chunks) {
        List<Document> docs = chunks.stream().map(this::toDocument).toList();
        embeddingIndexer.index(docs, null);
    }

    @Override
    public void clear() {
        embeddingIndexer.clear();
    }

    private DocumentChunk toChunk(Document doc) {
        Map<String, Object> metadata = doc.getMetadata() != null ? doc.getMetadata() : Map.of();
        int pageNumber = metadata.containsKey("page")
                ? ((Number) metadata.get("page")).intValue()
                : 0;
        String source = (String) metadata.getOrDefault("source", "unknown");
        return new DocumentChunk(doc.getId(), doc.getText(), pageNumber, source, metadata);
    }

    private Document toDocument(DocumentChunk chunk) {
        return new Document(chunk.id(), chunk.content(), chunk.metadata());
    }
}
