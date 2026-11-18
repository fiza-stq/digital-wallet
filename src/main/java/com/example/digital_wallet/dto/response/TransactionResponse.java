
package com.example.digital_wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        String senderWalletNumber,
        String receiverWalletNumber,
        BigDecimal amount,
        String currency,
        String type,
        String status,
        String description,
        LocalDateTime createdAt
) {}