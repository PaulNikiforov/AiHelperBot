package com.nikiforov.aichatbot.port.in;

import com.nikiforov.aichatbot.domain.exception.LlmUnavailableException;
import com.nikiforov.aichatbot.domain.model.Answer;
import com.nikiforov.aichatbot.domain.model.Question;

public interface AskQuestionUseCase {

    Answer ask(Question question);
}
