package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackIdTest {

    @Test
    void storesLongValue() {
        FeedbackId id = new FeedbackId(42L);
        assertThat(id.value()).isEqualTo(42L);
    }

    @Test
    void equalityBasedOnValue() {
        FeedbackId id1 = new FeedbackId(99L);
        FeedbackId id2 = new FeedbackId(99L);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
