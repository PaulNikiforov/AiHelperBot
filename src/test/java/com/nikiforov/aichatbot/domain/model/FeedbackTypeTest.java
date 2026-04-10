package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTypeTest {

    @Test
    void hasExactlyLikeAndDislikeValues() {
        assertThat(FeedbackType.values()).containsExactlyInAnyOrder(
                FeedbackType.LIKE,
                FeedbackType.DISLIKE
        );
    }
}
