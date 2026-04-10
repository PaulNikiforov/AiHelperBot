package com.nikiforov.aichatbot.adapter.out.persistence;

import com.nikiforov.aichatbot.port.out.FeedbackPersistencePort;
import com.nikiforov.aichatbot.port.out.FeedbackPersistencePortContractTest;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires database — run manually or via TestContainers")
class FeedbackPersistenceAdapterTest extends FeedbackPersistencePortContractTest {

    @Autowired
    private FeedbackPersistencePort feedbackPersistencePort;

    @Override
    public FeedbackPersistencePort createPort() {
        return feedbackPersistencePort;
    }
}
