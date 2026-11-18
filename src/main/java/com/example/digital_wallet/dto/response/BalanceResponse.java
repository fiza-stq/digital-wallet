
package com.example.digital_wallet.dto.response;

import java.math.BigDecimal;

public record BalanceResponse(
        String walletNumber,
        BigDecimal balance,
        String currency
) {}