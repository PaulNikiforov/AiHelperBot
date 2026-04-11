package com.nikiforov.aichatbot.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that permits all requests for integration tests.
 * <p>
 * This configuration is used by integration tests (IT classes) which use
 * TestRestTemplate to test the full application stack. For unit tests that
 * use MockMvc, JWT authentication is tested directly.
 * <p>
 * Using this approach allows integration tests to focus on business logic
 * and endpoint behavior without the complexity of JWT token management in
 * TestRestTemplate. JWT security is separately tested in JwtSecurityConfigTest.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
