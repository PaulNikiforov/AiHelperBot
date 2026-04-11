package com.nikiforov.aichatbot.adapter.in.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakJwtConverter Tests")
class KeycloakJwtConverterTest {

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Nested
    @DisplayName("Role extraction from realm_access")
    class RoleExtractionTests {

        @Test
        @DisplayName("should extract roles from realm_access.roles claim")
        void shouldExtractRolesFromRealmAccess() {
            Map<String, Object> realmAccess = Map.of("roles", List.of("user", "admin"));
            Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities())
                    .containsExactlyInAnyOrder(
                            new SimpleGrantedAuthority("ROLE_user"),
                            new SimpleGrantedAuthority("ROLE_admin")
                    );
        }

        @Test
        @DisplayName("should return empty authorities when realm_access claim is missing")
        void shouldReturnEmptyAuthoritiesWhenRealmAccessMissing() {
            Jwt jwt = createJwt(Map.of());

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should return empty authorities when roles claim is missing")
        void shouldReturnEmptyAuthoritiesWhenRolesMissing() {
            Map<String, Object> realmAccess = Map.of("other_claim", "value");
            Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should prefix roles with ROLE_")
        void shouldPrefixRolesWithROLE() {
            Map<String, Object> realmAccess = Map.of("roles", List.of("user"));
            Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities())
                    .allMatch(auth -> auth.getAuthority().startsWith("ROLE_"));
        }

        @Test
        @DisplayName("should use sub claim as principal name")
        void shouldUseSubAsPrincipalName() {
            // The Jwt builder sets sub to "testuser" by default in createJwt()
            Map<String, Object> claims = Map.of("realm_access", Map.of("roles", List.of("user")));
            Jwt jwt = createJwtWithCustomSub("user123", claims);

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getName()).isEqualTo("user123");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty roles list")
        void shouldHandleEmptyRolesList() {
            Map<String, Object> realmAccess = Map.of("roles", List.of());
            Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should filter non-string roles")
        void shouldFilterNonStringRoles() {
            // Create a mutable list for roles with mixed types (List.of() doesn't allow null)
            List<Object> mixedRoles = new java.util.ArrayList<>();
            mixedRoles.add("user");
            mixedRoles.add(123);
            mixedRoles.add("admin");

            Map<String, Object> realmAccess = Map.of("roles", mixedRoles);
            Jwt jwt = createJwt(Map.of("realm_access", realmAccess));

            var authToken = converter.convert(jwt);

            assertThat(authToken).isNotNull();
            assertThat(authToken.getAuthorities())
                    .containsExactlyInAnyOrder(
                            new SimpleGrantedAuthority("ROLE_user"),
                            new SimpleGrantedAuthority("ROLE_admin")
                    );
        }

        @Test
        @DisplayName("should throw exception when subject claim is null")
        void shouldThrowExceptionWhenSubjectIsNull() {
            // Create JWT without subject claim - builder will use null
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .claims(c -> c.put("realm_access", Map.of("roles", List.of("user"))))
                    .claim(JwtClaimNames.ISS, "http://localhost:8180/realms/ai-helper-bot")
                    // No sub claim set
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            // Should throw exception because subject is null
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("'sub'");
        }

        @Test
        @DisplayName("should throw exception when subject claim is blank")
        void shouldThrowExceptionWhenSubjectIsBlank() {
            // Create JWT with blank subject
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .claims(c -> c.put("realm_access", Map.of("roles", List.of("user"))))
                    .claim(JwtClaimNames.ISS, "http://localhost:8180/realms/ai-helper-bot")
                    .claim(JwtClaimNames.SUB, "   ")  // Blank subject
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            // Should throw exception because subject is blank
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.convert(jwt))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("'sub'");
        }
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .claim(JwtClaimNames.ISS, "http://localhost:8180/realms/ai-helper-bot")
                .claim(JwtClaimNames.SUB, "testuser")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt createJwtWithCustomSub(String sub, Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .claim(JwtClaimNames.ISS, "http://localhost:8180/realms/ai-helper-bot")
                .claim(JwtClaimNames.SUB, sub)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
