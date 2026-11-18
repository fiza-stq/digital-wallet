

package com.example.digital_wallet.mapper;

import com.example.digital_wallet.dto.response.TransactionResponse;
import com.example.digital_wallet.entity.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {}

    public static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getReference(),
                t.getSenderWallet()   != null ? t.getSenderWallet().getWalletNumber()   : null,
                t.getReceiverWallet() != null ? t.getReceiverWallet().getWalletNumber() : null,
                t.getAmount(),
                t.getCurrency(),
                t.getType().name(),
                t.getStatus().name(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}