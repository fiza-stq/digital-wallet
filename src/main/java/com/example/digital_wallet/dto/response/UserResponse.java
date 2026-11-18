
package com.example.digital_wallet.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Set<String> roles,
        LocalDateTime createdAt
) {}