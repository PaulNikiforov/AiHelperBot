package com.nikiforov.aichatbot.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("JWT Security Configuration Tests")
class JwtSecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/ask endpoint")
    class AskEndpointTests {

        @Test
        @DisplayName("should return 401 when no token is provided")
        void askWithoutToken_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"test question\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should not return 401 when valid JWT is provided")
        void askWithValidJwt_not401() throws Exception {
            mockMvc.perform(post("/api/v1/ask")
                            .with(jwt().jwt(jwt -> jwt
                                    .claim("email", "test@example.com")
                                    .subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"test question\"}"))
                    .andExpect(result -> org.hamcrest.MatcherAssert.assertThat(
                            result.getResponse().getStatus(),
                            not(org.hamcrest.Matchers.equalTo(401))
                    ));
        }

        @Test
        @DisplayName("should not return 401 when JWT has any issuer (mock JWT bypasses validation)")
        void askWithAnyIssuer_not401() throws Exception {
            mockMvc.perform(post("/api/v1/ask")
                            .with(jwt().jwt(jwt -> jwt
                                    .issuer("http://any-issuer.com")
                                    .claim("email", "test@example.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"test question\"}"))
                    .andExpect(result -> org.hamcrest.MatcherAssert.assertThat(
                            result.getResponse().getStatus(),
                            not(org.hamcrest.Matchers.equalTo(401))
                    ));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/botfeedback endpoint")
    class FeedbackEndpointTests {

        @Test
        @DisplayName("should return 401 when no token is provided")
        void feedbackWithoutToken_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/botfeedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answer\":\"test\",\"feedbackType\":\"LIKE\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should not return 401 when valid JWT is provided")
        void feedbackWithValidJwt_not401() throws Exception {
            mockMvc.perform(post("/api/v1/botfeedback")
                            .with(jwt().jwt(jwt -> jwt
                                    .claim("email", "test@example.com")
                                    .subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"test question\",\"answer\":\"test answer\",\"botFeedbackType\":\"LIKE\"}"))
                    .andExpect(result -> org.hamcrest.MatcherAssert.assertThat(
                            result.getResponse().getStatus(),
                            not(org.hamcrest.Matchers.equalTo(401))
                    ));
        }
    }

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpointTests {

        @Test
        @DisplayName("/actuator/health should be accessible without token")
        void actuatorHealth_withoutToken_returns200() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/health/liveness should be accessible without token")
        void actuatorLiveness_withoutToken_returns200() throws Exception {
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Swagger UI should be accessible without token")
        void swaggerUi_withoutToken_returns200() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OpenAPI docs should be accessible without token")
        void openApiDocs_withoutToken_returns200() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }
}
