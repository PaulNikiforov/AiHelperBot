package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.model.QueryType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryClassifierTest {

    private final QueryClassifier classifier = new QueryClassifier();

    @Test
    void supportQuery() {
        QueryType result = classifier.classify("I have an error in my review");
        assertThat(result).isEqualTo(QueryType.SUPPORT);
    }

    @Test
    void reviewProcessQuery() {
        QueryType result = classifier.classify("How do I schedule a review?");
        assertThat(result).isEqualTo(QueryType.REVIEW_PROCESS);
    }

    @Test
    void peersQuery() {
        QueryType result = classifier.classify("How do I select peers for assessment?");
        assertThat(result).isEqualTo(QueryType.PEERS);
    }

    @Test
    void remindPeersQuery() {
        QueryType result = classifier.classify("How do I remind peers to submit?");
        assertThat(result).isEqualTo(QueryType.REMIND_PEERS);
    }

    @Test
    void inboxQuery() {
        QueryType result = classifier.classify("Where do I find my inbox notifications?");
        assertThat(result).isEqualTo(QueryType.INBOX);
    }

    @Test
    void growthPlanQuery() {
        QueryType result = classifier.classify("Tell me about growth plan tasks");
        assertThat(result).isEqualTo(QueryType.GROWTH_PLAN);
    }

    @Test
    void timeRelatedQuery() {
        QueryType result = classifier.classify("What is the deadline for reviews?");
        assertThat(result).isEqualTo(QueryType.TIME_RELATED);
    }

    @Test
    void definitionQuery() {
        QueryType result = classifier.classify("What is a review?");
        assertThat(result).isEqualTo(QueryType.DEFINITION);
    }

    @Test
    void generalQuery() {
        QueryType result = classifier.classify("Tell me about the system");
        assertThat(result).isEqualTo(QueryType.GENERAL);
    }

    @Test
    void extractTerm_definitionQuery_returnsTerm() {
        String term = classifier.extractTerm("What is a review?", false);
        assertThat(term).isEqualTo("review");
    }

    @Test
    void extractTerm_timeQuery_returnsTerm() {
        String term = classifier.extractTerm("When is the deadline?", true);
        assertThat(term).isNotBlank();
    }

    @Test
    void extractTerm_tooManyWords_fallsBackToGeneral() {
        QueryType result = classifier.classify("What is the very long complex multi word term?");
        assertThat(result).isEqualTo(QueryType.GENERAL);
    }
}
