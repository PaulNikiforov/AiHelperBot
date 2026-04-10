package com.nikiforov.aichatbot.adapter.out.vectorstore;

import com.nikiforov.aichatbot.port.out.VectorSearchPort;
import com.nikiforov.aichatbot.port.out.VectorSearchPortContractTest;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running Ollama — run manually")
class InMemoryVectorStoreAdapterTest extends VectorSearchPortContractTest {

    @Autowired
    private VectorSearchPort vectorSearchPort;

    @Override
    public VectorSearchPort createSearchPort() {
        return vectorSearchPort;
    }
}
