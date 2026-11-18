
package com.example.digital_wallet.dto.request;

import jakarta.validation.constraints.*;

        import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Receiver wallet number is required")
        String receiverWalletNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 15, fraction = 4, message = "Amount format invalid")
        BigDecimal amount,

        @Size(max = 255, message = "Description too long")
        String description,

        @Size(max = 64, message = "Idempotency key too long")
        String idempotencyKey   // optional but recommended
) {}