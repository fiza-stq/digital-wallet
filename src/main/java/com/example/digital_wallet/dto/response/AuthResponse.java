
package com.example.digital_wallet.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {}
