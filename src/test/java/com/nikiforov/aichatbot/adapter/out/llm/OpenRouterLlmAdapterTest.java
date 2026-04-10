package com.nikiforov.aichatbot.adapter.out.llm;

import com.nikiforov.aichatbot.port.out.LlmPort;
import com.nikiforov.aichatbot.port.out.LlmPortContractTest;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running OpenRouter API — run manually")
class OpenRouterLlmAdapterTest extends LlmPortContractTest {

    @Autowired
    private LlmPort llmPort;

    @Override
    public LlmPort createPort() {
        return llmPort;
    }
}
