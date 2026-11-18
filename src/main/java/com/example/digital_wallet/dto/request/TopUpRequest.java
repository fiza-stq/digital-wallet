
package com.example.digital_wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum top-up is 1.00")
        @Digits(integer = 15, fraction = 4, message = "Amount format invalid")
        BigDecimal amount
) {}