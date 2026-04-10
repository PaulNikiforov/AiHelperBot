package com.nikiforov.aichatbot.adapter.in.web;

import com.nikiforov.aichatbot.port.in.GetBotIntroUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BotIntroControllerAdapter.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = "app.adapters.inbound.enabled=true")
class BotIntroControllerAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetBotIntroUseCase getBotIntroUseCase;

    @Test
    void getIntro_returns200WithIntroText() throws Exception {
        when(getBotIntroUseCase.getIntroText()).thenReturn("Hello! I'm the AI Helper Bot...");

        mockMvc.perform(get("/api/v1/bot/intro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.introText").value("Hello! I'm the AI Helper Bot..."));
    }

    @Test
    void getIntro_nullText_returns200WithNull() throws Exception {
        when(getBotIntroUseCase.getIntroText()).thenReturn(null);

        mockMvc.perform(get("/api/v1/bot/intro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.introText").isEmpty());
    }
}
