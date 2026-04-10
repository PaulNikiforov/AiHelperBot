package com.nikiforov.aichatbot.repository;

import com.nikiforov.aichatbot.model.BotFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotFeedbackRepository extends JpaRepository<BotFeedback, Long> {
}
