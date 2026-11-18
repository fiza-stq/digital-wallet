

package com.example.digital_wallet.security;

import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    /**
     * Returns the User currently authenticated in this request.
     * Throws 401 if somehow no authentication is present.
     */
    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            throw new UnauthorizedException("No authenticated user in context");
        }
        return details.getUser();
    }
}