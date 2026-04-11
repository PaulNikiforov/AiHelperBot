package com.nikiforov.aichatbot.adapter.out.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpringSecurityIdentityAdapter Tests")
class SpringSecurityIdentityAdapterTest {

    private final SpringSecurityIdentityAdapter adapter = new SpringSecurityIdentityAdapter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("When no authentication")
    class NoAuthenticationTests {

        @Test
        @DisplayName("should return null when no authentication exists")
        void noAuthentication_returnsNull() {
            String email = adapter.getCurrentUserEmail();
            assertThat(email).isNull();
        }

        @Test
        @DisplayName("should return null when anonymous user")
        void anonymousUser_returnsNull() {
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "anonymousUser", null, List.of()
                    )
            );

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isNull();
        }
    }

    @Nested
    @DisplayName("When JWT with email claim")
    class JwtWithEmailTests {

        @Test
        @DisplayName("should return email from JWT email claim")
        void jwtWithEmailClaim_returnsEmail() {
            Jwt jwt = createJwt(Map.of("email", "test@example.com"));
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should prefer email over preferred_username")
        void jwtWithEmailAndPreferredUsername_returnsEmail() {
            Jwt jwt = createJwt(Map.of(
                    "email", "test@example.com",
                    "preferred_username", "testuser"
            ));
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return null for blank email claim")
        void jwtWithBlankEmail_returnsPreferredUsername() {
            Jwt jwt = createJwt(Map.of(
                    "email", "   ",
                    "preferred_username", "testuser"
            ));
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("When JWT with preferred_username only")
    class JwtWithPreferredUsernameTests {

        @Test
        @DisplayName("should return preferred_username when email is absent")
        void jwtWithPreferredUsernameOnly_returnsPreferredUsername() {
            Jwt jwt = createJwt(Map.of("preferred_username", "testuser"));
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should return sub when both email and preferred_username are absent")
        void jwtWithSubOnly_returnsSub() {
            Jwt jwt = createJwtWithSubject("user123", Map.of());
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("user123");
        }

        @Test
        @DisplayName("should throw exception when JWT has no valid claims")
        void jwtWithNoValidClaims_throwsException() {
            // Create JWT with null/blank sub and no other claims
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .claim("iss", "http://localhost:8180/realms/ai-helper-bot")
                    // No email, no preferred_username, sub will be null
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Should throw exception because no valid claim found
            assertThatThrownBy(() -> adapter.getCurrentUserEmail())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("email");
        }
    }

    @Nested
    @DisplayName("When non-JWT authentication")
    class NonJwtAuthenticationTests {

        @Test
        @DisplayName("should return authentication name for basic auth")
        void basicAuth_returnsUsername() {
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "john.doe", null, List.of()
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            String email = adapter.getCurrentUserEmail();
            assertThat(email).isEqualTo("john.doe");
        }
    }

    private Jwt createJwt(java.util.Map<String, Object> claims) {
        return createJwtWithSubject("testuser", claims);
    }

    private Jwt createJwtWithSubject(String sub, java.util.Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .claim("iss", "http://localhost:8180/realms/ai-helper-bot")
                .claim("sub", sub)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
