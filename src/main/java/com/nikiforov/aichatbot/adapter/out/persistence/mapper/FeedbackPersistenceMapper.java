package com.nikiforov.aichatbot.adapter.out.persistence.mapper;

import com.nikiforov.aichatbot.domain.model.Feedback;
import com.nikiforov.aichatbot.domain.model.FeedbackId;
import com.nikiforov.aichatbot.domain.model.FeedbackType;
import com.nikiforov.aichatbot.adapter.out.persistence.entity.BotFeedback;
import com.nikiforov.aichatbot.adapter.out.persistence.entity.BotFeedbackType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface FeedbackPersistenceMapper {

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "botFeedbackType", source = "type")
    @Mapping(target = "employeeEmail", source = "employeeEmail")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToLocalDateTime")
    BotFeedback toEntity(Feedback feedback);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "type", source = "botFeedbackType", qualifiedByName = "toDomainFeedbackType")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localDateTimeToInstant")
    Feedback toDomain(BotFeedback entity);

    default Long mapFeedbackId(FeedbackId feedbackId) {
        return feedbackId != null ? feedbackId.value() : null;
    }

    default FeedbackId mapLongToFeedbackId(Long id) {
        return id != null ? new FeedbackId(id) : null;
    }

    default BotFeedbackType mapFeedbackType(FeedbackType type) {
        if (type == null) return null;
        return switch (type) {
            case LIKE -> BotFeedbackType.LIKE;
            case DISLIKE -> BotFeedbackType.DISLIKE;
        };
    }

    @org.mapstruct.Named("toDomainFeedbackType")
    default FeedbackType mapBotFeedbackType(BotFeedbackType type) {
        if (type == null) return null;
        return switch (type) {
            case LIKE -> FeedbackType.LIKE;
            case DISLIKE -> FeedbackType.DISLIKE;
        };
    }

    @org.mapstruct.Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }

    @org.mapstruct.Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime ldt) {
        return ldt != null ? ldt.toInstant(ZoneOffset.UTC) : null;
    }
}
