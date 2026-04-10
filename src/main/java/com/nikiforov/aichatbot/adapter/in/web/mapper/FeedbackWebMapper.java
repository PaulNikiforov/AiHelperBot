package com.nikiforov.aichatbot.adapter.in.web.mapper;

import com.nikiforov.aichatbot.adapter.in.web.dto.FeedbackResponse;
import com.nikiforov.aichatbot.domain.model.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackWebMapper {

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "botFeedbackType", source = "type")
    FeedbackResponse toResponse(Feedback feedback);
}
