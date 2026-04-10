package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class FeedbackPersistencePortContractTest {

    public abstract FeedbackPersistencePort createPort();

    @Test
    void save_andFindById_roundTrip() {
        FeedbackPersistencePort port = createPort();
        Feedback feedback = new Feedback(
                null, "What is X?", "X is a thing", FeedbackType.LIKE, "user@test.com", null
        );

        Feedback saved = port.save(feedback);
        assertThat(saved.getId()).isNotNull();

        Optional<Feedback> found = port.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getQuestion()).isEqualTo("What is X?");
        assertThat(found.get().getType()).isEqualTo(FeedbackType.LIKE);
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        FeedbackPersistencePort port = createPort();
        assertThat(port.findById(new FeedbackId(999999L))).isEmpty();
    }
}
