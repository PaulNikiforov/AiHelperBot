package com.nikiforov.aichatbot.adapter.in.web.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converter that extracts Keycloak-specific claims from a JWT and builds
 * a Spring Security {@link JwtAuthenticationToken}.
 * <p>
 * This converter handles the Keycloak-specific structure where roles are
 * nested under {@code realm_access.roles} in the JWT claims.
 * <p>
 * Roles are automatically prefixed with {@code ROLE_} to match Spring Security's
 * convention for role-based authorization (e.g., {@code @PreAuthorize("hasRole('admin')")}).
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtConverter.class);
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getClaimAsString("sub");
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("JWT must contain a non-blank 'sub' claim");
        }
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, subject);
    }

    /**
     * Extracts authorities from the Keycloak JWT.
     * <p>
     * Keycloak stores realm roles in the {@code realm_access.roles} claim:
     * <pre>
     * {
     *   "realm_access": {
     *     "roles": ["user", "admin"]
     *   }
     * }
     * </pre>
     *
     * @param jwt the JWT token
     * @return collection of granted authorities with ROLE_ prefix
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            log.warn("JWT missing {} claim - no authorities granted", REALM_ACCESS_CLAIM);
            return List.of();
        }

        Object rolesObj = realmAccess.get(ROLES_CLAIM);
        if (rolesObj == null) {
            log.warn("JWT missing {}.{} claim - no authorities granted", REALM_ACCESS_CLAIM, ROLES_CLAIM);
            return List.of();
        }

        if (!(rolesObj instanceof Collection<?>)) {
            log.warn("JWT {} claim is not a Collection - no authorities granted", ROLES_CLAIM);
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Collection<?> roles = (Collection<?>) rolesObj;
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> {
                    if (!role.matches("^[a-zA-Z0-9_-]+$")) {
                        log.warn("Invalid role format: {}", role);
                        return null;
                    }
                    return ROLE_PREFIX + role;
                })
                .filter(java.util.Objects::nonNull)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
