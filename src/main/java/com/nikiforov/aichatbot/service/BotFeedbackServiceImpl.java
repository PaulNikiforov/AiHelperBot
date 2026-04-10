package com.nikiforov.aichatbot.service;

import com.nikiforov.aichatbot.dto.request.BotFeedbackRequest;
import com.nikiforov.aichatbot.dto.response.BotFeedbackResponse;
import com.nikiforov.aichatbot.exceptionhandler.ErrorCodes;
import com.nikiforov.aichatbot.exceptionhandler.exception.RestException;
import com.nikiforov.aichatbot.mapper.BotFeedbackMapper;
import com.nikiforov.aichatbot.model.BotFeedback;
import com.nikiforov.aichatbot.repository.BotFeedbackRepository;
import com.nikiforov.aichatbot.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BotFeedbackServiceImpl implements BotFeedbackService {

    private static final int MAX_ANSWER_LENGTH = 10_000;

    private final BotFeedbackRepository botFeedbackRepository;
    private final BotFeedbackMapper botFeedbackMapper;
    private final SecurityUtils securityUtils;

    @Transactional
    public BotFeedbackResponse save(BotFeedbackRequest request) {
        BotFeedback entity = botFeedbackMapper.toEntity(request);
        entity.setAnswer(truncate(request.answer(), MAX_ANSWER_LENGTH));
        entity.setEmployeeEmail(securityUtils.getUserName());

        entity = botFeedbackRepository.save(entity);
        return botFeedbackMapper.toDto(entity);
    }

    private static String truncate(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Transactional(readOnly = true)
    public BotFeedbackResponse findById(Long id) {
        return botFeedbackRepository.findById(id)
                .map(botFeedbackMapper::toDto)
                .orElseThrow(() -> new RestException(ErrorCodes.BOT_FEEDBACK_NOT_FOUND));
    }
}
