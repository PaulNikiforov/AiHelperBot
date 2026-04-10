package com.nikiforov.aichatbot.domain.service;

import com.nikiforov.aichatbot.domain.exception.FeedbackNotFoundException;
import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.port.out.FeedbackPersistencePort;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackServiceTest {

    private final InMemoryFeedbackPersistence persistence = new InMemoryFeedbackPersistence();
    private final FeedbackService service = new FeedbackService(persistence);

    @Test
    void save_truncatesAnswerToLimit() {
        String longAnswer = "a".repeat(10_001);
        Feedback saved = service.save("Q?", longAnswer, FeedbackType.LIKE, null);
        assertThat(saved.getAnswer()).hasSize(10_000);
    }

    @Test
    void save_answerAtExactLimit_notTruncated() {
        String exactAnswer = "a".repeat(10_000);
        Feedback saved = service.save("Q?", exactAnswer, FeedbackType.LIKE, null);
        assertThat(saved.getAnswer()).isEqualTo(exactAnswer);
    }

    @Test
    void save_delegatesToPersistence() {
        Feedback saved = service.save("Q?", "A", FeedbackType.LIKE, "user@example.com");
        assertThat(saved.getQuestion()).isEqualTo("Q?");
        assertThat(saved.getAnswer()).isEqualTo("A");
        assertThat(saved.getType()).isEqualTo(FeedbackType.LIKE);
        assertThat(saved.getEmployeeEmail()).isEqualTo("user@example.com");
        assertThat(persistence.store).containsKey(saved.getId());
    }

    @Test
    void getById_found_returnsFeedback() {
        Feedback saved = persistence.saveDirect(new Feedback(
                new FeedbackId(1L), "Q", "A", FeedbackType.LIKE, null, null));

        Feedback found = service.getById(new FeedbackId(1L));
        assertThat(found.getQuestion()).isEqualTo("Q");
    }

    @Test
    void getById_notFound_throwsException() {
        assertThatThrownBy(() -> service.getById(new FeedbackId(999L)))
                .isInstanceOf(FeedbackNotFoundException.class);
    }

    private static class InMemoryFeedbackPersistence implements FeedbackPersistencePort {
        final Map<FeedbackId, Feedback> store = new HashMap<>();

        @Override
        public Feedback save(Feedback feedback) {
            FeedbackId id = feedback.getId() != null ? feedback.getId() : new FeedbackId((long) store.size() + 1);
            Feedback withId = new Feedback(id, feedback.getQuestion(), feedback.getAnswer(),
                    feedback.getType(), feedback.getEmployeeEmail(), feedback.getCreatedAt());
            store.put(id, withId);
            return withId;
        }

        @Override
        public Optional<Feedback> findById(FeedbackId id) {
            return Optional.ofNullable(store.get(id));
        }

        Feedback saveDirect(Feedback feedback) {
            store.put(feedback.getId(), feedback);
            return feedback;
        }
    }
}
