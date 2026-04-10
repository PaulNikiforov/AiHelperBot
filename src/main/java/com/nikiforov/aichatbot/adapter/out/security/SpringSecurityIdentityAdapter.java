package com.nikiforov.aichatbot.adapter.out.security;

import com.nikiforov.aichatbot.port.out.IdentityProviderPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Adapter that provides the current authenticated user's email from Spring Security.
 * <p>
 * <strong>Null Safety:</strong> Returns {@code null} if no user is authenticated or if
 * the authentication object is not available. Callers must handle null appropriately,
 * typically by accepting anonymous feedback or throwing an unauthorized exception.
 * <p>
 * Previously wrapped SecurityUtils - inlined as part of Phase 10 cleanup.
 */
@Component
public class SpringSecurityIdentityAdapter implements IdentityProviderPort {

    @Override
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return null;
    }
}
