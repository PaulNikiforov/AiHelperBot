package com.nikiforov.aichatbot.adapter.in.web.mapper;

import com.nikiforov.aichatbot.adapter.in.web.dto.AskRequest;
import com.nikiforov.aichatbot.adapter.in.web.dto.AskResponse;
import com.nikiforov.aichatbot.domain.model.Answer;
import com.nikiforov.aichatbot.domain.model.Question;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionWebMapper {

    default Question toQuestion(AskRequest request) {
        return new Question(request.question());
    }

    default AskResponse toAskResponse(Answer answer) {
        return new AskResponse(answer.text());
    }
}
