package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.DocumentChunk;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DocumentRanker {

    private static final Pattern TIME_SCORE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2} months?|\\d{1,2} "
                    + "days?|deadline|schedule|time|period|Q[1-4]|launch|launched|scheduled)\\b");

    public List<DocumentChunk> rankByKeywords(List<DocumentChunk> documents, String query, int maxDocuments) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        return documents.stream()
                .sorted(Comparator.comparingInt(
                        (DocumentChunk doc) -> calculateKeywordScore(doc, queryWords)).reversed())
                .limit(maxDocuments)
                .toList();
    }

    public List<DocumentChunk> rankByTimeRelevance(List<DocumentChunk> documents, String query, int maxDocuments) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        return documents.stream()
                .sorted(Comparator.comparingInt(
                        (DocumentChunk doc) ->
                                calculateKeywordScore(doc, queryWords) + calculateTimeScore(doc)).reversed())
                .limit(maxDocuments)
                .toList();
    }

    public List<DocumentChunk> mergeWithPage(List<DocumentChunk> baseDocs,
                                              List<DocumentChunk> pageDocs,
                                              int maxDocuments) {
        return Stream.concat(pageDocs.stream(), baseDocs.stream())
                .distinct()
                .limit(maxDocuments)
                .toList();
    }

    private int calculateKeywordScore(DocumentChunk doc, String[] queryWords) {
        String text = doc.content().toLowerCase();
        int score = 0;
        for (String word : queryWords) {
            if (text.contains(word)) score++;
        }
        return score;
    }

    private int calculateTimeScore(DocumentChunk doc) {
        var matcher = TIME_SCORE_PATTERN.matcher(doc.content());
        int score = 0;
        while (matcher.find()) score++;
        return score;
    }
}
