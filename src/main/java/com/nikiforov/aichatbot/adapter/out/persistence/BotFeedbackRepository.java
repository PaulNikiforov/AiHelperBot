package com.nikiforov.aichatbot.adapter.out.persistence;

import com.nikiforov.aichatbot.adapter.out.persistence.entity.BotFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotFeedbackRepository extends JpaRepository<BotFeedback, Long> {
}
