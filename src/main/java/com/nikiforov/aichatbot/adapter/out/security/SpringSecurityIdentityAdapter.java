package com.nikiforov.aichatbot.adapter.out.security;

import com.nikiforov.aichatbot.port.out.IdentityProviderPort;
import com.nikiforov.aichatbot.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSecurityIdentityAdapter implements IdentityProviderPort {

    private final SecurityUtils securityUtils;

    @Override
    public String getCurrentUserEmail() {
        return securityUtils.getUserName();
    }
}
