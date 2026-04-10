package com.nikiforov.aichatbot.adapter.out.persistence;

import com.nikiforov.aichatbot.adapter.out.persistence.entity.BotFeedback;
import com.nikiforov.aichatbot.adapter.out.persistence.mapper.FeedbackPersistenceMapper;
import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.port.out.FeedbackPersistencePort;
import com.nikiforov.aichatbot.adapter.out.persistence.BotFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements FeedbackPersistencePort {

    private final BotFeedbackRepository repository;
    private final FeedbackPersistenceMapper mapper;

    @Override
    @Transactional
    public Feedback save(Feedback feedback) {
        BotFeedback entity = mapper.toEntity(feedback);
        BotFeedback saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Feedback> findById(FeedbackId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }
}
