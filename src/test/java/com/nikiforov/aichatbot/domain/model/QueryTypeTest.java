package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryTypeTest {

    @Test
    void hasExactlyNineExpectedValues() {
        assertThat(QueryType.values()).containsExactlyInAnyOrder(
                QueryType.GENERAL,
                QueryType.DEFINITION,
                QueryType.TIME_RELATED,
                QueryType.SUPPORT,
                QueryType.INBOX,
                QueryType.PEERS,
                QueryType.REVIEW_PROCESS,
                QueryType.REMIND_PEERS,
                QueryType.GROWTH_PLAN
        );
    }
}
