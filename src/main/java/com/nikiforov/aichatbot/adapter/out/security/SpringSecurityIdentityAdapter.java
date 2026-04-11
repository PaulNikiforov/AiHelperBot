package com.nikiforov.aichatbot.adapter.out.security;

import com.nikiforov.aichatbot.port.out.IdentityProviderPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Adapter that provides the current authenticated user's email from Spring Security.
 * <p>
 * <strong>JWT Claim Extraction:</strong> When using JWT authentication (Keycloak),
 * extracts the email from the JWT claims in this priority order:
 * <ol>
 *   <li>{@code email} claim - the user's email address</li>
 *   <li>{@code preferred_username} claim - fallback username</li>
 *   <li>{@code sub} claim - subject identifier as last resort</li>
 * </ol>
 * <p>
 * <strong>Null Safety:</strong> Returns {@code null} if no user is authenticated or if
 * the authentication object is not available. Callers must handle null appropriately,
 * typically by accepting anonymous feedback or throwing an unauthorized exception.
 */
@Component
public class SpringSecurityIdentityAdapter implements IdentityProviderPort {

    private static final String EMAIL_CLAIM = "email";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";

    @Override
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        // If authentication is a JWT token, extract email from claims
        if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
            return extractEmailFromJwt(jwtAuthToken.getToken());
        }

        // Fall back to authentication name for non-JWT authentication
        return authentication.getName();
    }

    /**
     * Extracts email from JWT claims with fallback strategy.
     *
     * @param jwt the JWT token
     * @return email, preferred_username, or sub claim (in that priority order)
     * @throws IllegalStateException if no valid identifier claim is found
     */
    private String extractEmailFromJwt(Jwt jwt) {
        // Try email claim first
        String email = jwt.getClaim(EMAIL_CLAIM);
        if (email != null && !email.isBlank()) {
            return email;
        }

        // Fall back to preferred_username
        String preferredUsername = jwt.getClaim(PREFERRED_USERNAME_CLAIM);
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        // Last resort: use subject (must be non-null and non-blank)
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }

        throw new IllegalStateException(
                "JWT must contain at least one of: email, preferred_username, or sub claim"
        );
    }
}
