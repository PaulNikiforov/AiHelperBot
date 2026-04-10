package com.nikiforov.aichatbot.mapper;

import com.nikiforov.aichatbot.dto.request.BotFeedbackRequest;
import com.nikiforov.aichatbot.dto.response.BotFeedbackResponse;
import com.nikiforov.aichatbot.model.BotFeedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BotFeedbackMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeEmail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "answer", ignore = true)
    BotFeedback toEntity(BotFeedbackRequest dto);

    BotFeedbackResponse toDto(BotFeedback entity);
}
