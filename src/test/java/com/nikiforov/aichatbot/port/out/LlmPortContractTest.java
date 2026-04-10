package com.nikiforov.aichatbot.port.out;

import com.nikiforov.aichatbot.domain.model.LlmResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class LlmPortContractTest {

    public abstract LlmPort createPort();

    @Test
    void ask_returnsNonNullLlmResponse() {
        LlmPort port = createPort();

        LlmResponse response = port.ask("You are a helpful assistant.", "What is 2+2?");

        assertThat(response).isNotNull();
        assertThat(response.answerText()).isNotBlank();
    }

    @Test
    void id_returnsNonNullString() {
        LlmPort port = createPort();

        String id = port.id();

        assertThat(id).isNotBlank();
    }

    @Test
    void isAvailable_returnsBoolean() {
        LlmPort port = createPort();

        boolean available = port.isAvailable();

        assertThat(available).isTrue();
    }
}
