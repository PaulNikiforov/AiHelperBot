package com.nikiforov.aichatbot.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    @Test
    void rejectsNullText() {
        assertThatThrownBy(() -> new Question(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> new Question("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsValidText() {
        Question q = new Question("What is a growth plan?");
        assertThat(q.text()).isEqualTo("What is a growth plan?");
    }
}
