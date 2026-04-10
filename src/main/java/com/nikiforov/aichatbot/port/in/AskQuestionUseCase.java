package com.nikiforov.aichatbot.port.in;

import com.nikiforov.aichatbot.domain.model.Answer;
import com.nikiforov.aichatbot.domain.model.Question;

public interface AskQuestionUseCase {

    /**
     * @throws com.nikiforov.aichatbot.domain.exception.LlmUnavailableException if all LLM providers fail
     */
    Answer ask(Question question);
}
