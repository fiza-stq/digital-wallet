package com.example.digital_wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        String walletNumber,
        BigDecimal balance,
        String currency,
        String status,
        LocalDateTime createdAt
) {}