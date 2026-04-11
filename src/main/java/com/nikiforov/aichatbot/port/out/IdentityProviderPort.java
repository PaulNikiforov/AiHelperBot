package com.nikiforov.aichatbot.port.out;

/**
 * Port for retrieving the current authenticated user's identity.
 * <p>
 * Implementations must extract user identification from the security context
 * (e.g., JWT claims, session data) and return it as a String.
 * <p>
 * <strong>Null Handling:</strong> Returns {@code null} when no user is authenticated
 * or when the authentication object does not contain the expected claims. Callers
 * should handle this case appropriately, typically by:
 * <ul>
 *   <li>Rejecting the request with {@code 401 Unauthorized} (for operations requiring authentication)</li>
 *   <li>Accepting anonymous feedback (for scenarios where user identity is optional)</li>
 * </ul>
 */
public interface IdentityProviderPort {

    /**
     * Returns the email address or identifier of the currently authenticated user.
     *
     * @return the user's email/identifier, or {@code null} if not authenticated
     */
    String getCurrentUserEmail();
}
