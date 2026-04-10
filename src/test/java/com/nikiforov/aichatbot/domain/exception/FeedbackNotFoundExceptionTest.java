package com.nikiforov.aichatbot.domain.exception;

import com.nikiforov.aichatbot.domain.model.FeedbackId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackNotFoundExceptionTest {

    @Test
    void extendsDomainException() {
        FeedbackNotFoundException ex = new FeedbackNotFoundException(new FeedbackId(42L));
        assertThat(ex).isInstanceOf(DomainException.class);
    }

    @Test
    void constructsWitFeedbackIdInMessage() {
        FeedbackNotFoundException ex = new FeedbackNotFoundException(new FeedbackId(42L));
        assertThat(ex.getMessage()).contains("42");
    }
}
