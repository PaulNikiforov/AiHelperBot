package com.nikiforov.aichatbot.config;

import com.nikiforov.aichatbot.config.properties.RagValidationProperties;
import com.nikiforov.aichatbot.domain.service.*;
import com.nikiforov.aichatbot.domain.validation.FormatValidator;
import com.nikiforov.aichatbot.domain.validation.InputValidationChain;
import com.nikiforov.aichatbot.port.in.*;
import com.nikiforov.aichatbot.port.out.FeedbackPersistencePort;
import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.port.out.VectorSearchPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BeanConfiguration {

    @Bean
    public FormatValidator formatValidator(RagValidationProperties validationProperties) {
        return new FormatValidator(validationProperties.getQuickFilter().getMinLength());
    }

    @Bean
    public InputValidationChain inputValidationChain(FormatValidator formatValidator) {
        return new InputValidationChain(List.of(formatValidator));
    }

    @Bean
    public QueryClassifier queryClassifier() {
        return new QueryClassifier();
    }

    @Bean
    public DocumentRanker domainDocumentRanker() {
        return new DocumentRanker();
    }

    @Bean
    public PromptAssembler promptAssembler() {
        return new PromptAssembler();
    }

    @Bean
    public AskQuestionUseCase askQuestionUseCase(InputValidationChain validationChain,
                                                  QueryClassifier queryClassifier,
                                                  VectorSearchPort vectorSearch,
                                                  DocumentRanker domainDocumentRanker,
                                                  PromptAssembler promptAssembler,
                                                  LlmPort llmPort) {
        return new RagOrchestrator(validationChain, queryClassifier, vectorSearch,
                domainDocumentRanker, promptAssembler, llmPort);
    }

    @Bean
    public FeedbackService feedbackService(FeedbackPersistencePort persistencePort) {
        return new FeedbackService(persistencePort);
    }

    @Bean
    public SaveFeedbackUseCase saveFeedbackUseCase(FeedbackService feedbackService) {
        return feedbackService;
    }

    @Bean
    public GetFeedbackUseCase getFeedbackUseCase(FeedbackService feedbackService) {
        return feedbackService;
    }

}
