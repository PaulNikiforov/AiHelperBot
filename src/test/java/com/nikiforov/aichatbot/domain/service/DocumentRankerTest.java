package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRankerTest {

    private final DocumentRanker ranker = new DocumentRanker();

    private DocumentChunk chunk(String id, String content, int page) {
        return new DocumentChunk(id, content, page, "guide.pdf", Map.of());
    }

    @Test
    void rankByKeywords_scoresByQueryWordOverlap() {
        List<DocumentChunk> docs = List.of(
                chunk("1", "review process deadline schedule", 1),
                chunk("2", "unrelated content here", 2),
                chunk("3", "review deadline is coming", 3)
        );

        List<DocumentChunk> ranked = ranker.rankByKeywords(docs, "review deadline", 3);

        assertThat(ranked).hasSize(3);
        assertThat(ranked.get(0).id()).isEqualTo("1");
    }

    @Test
    void rankByKeywords_respectsMaxDocuments() {
        List<DocumentChunk> docs = List.of(
                chunk("1", "review", 1),
                chunk("2", "review", 2),
                chunk("3", "review", 3)
        );

        List<DocumentChunk> ranked = ranker.rankByKeywords(docs, "review", 2);
        assertThat(ranked).hasSize(2);
    }

    @Test
    void rankByTimeRelevance_boostsDatePatterns() {
        List<DocumentChunk> docs = List.of(
                chunk("1", "review process information", 1),
                chunk("2", "deadline is 01/01/2024 schedule", 2)
        );

        List<DocumentChunk> ranked = ranker.rankByTimeRelevance(docs, "deadline", 2);

        assertThat(ranked.get(0).id()).isEqualTo("2");
    }

    @Test
    void mergeWithPage_prependsPageDocs_deduplicates_limits() {
        List<DocumentChunk> baseDocs = List.of(
                chunk("1", "base content", 1),
                chunk("2", "more base", 2)
        );
        List<DocumentChunk> pageDocs = List.of(
                chunk("3", "page content", 5),
                chunk("1", "base content", 1)
        );

        List<DocumentChunk> merged = ranker.mergeWithPage(baseDocs, pageDocs, 3);

        assertThat(merged).hasSize(3);
        assertThat(merged.get(0).id()).isEqualTo("3");
        assertThat(merged.stream().map(DocumentChunk::id).distinct().count())
                .isEqualTo(merged.size());
    }
}
