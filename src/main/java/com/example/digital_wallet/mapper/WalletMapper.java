
package com.example.digital_wallet.mapper;

import com.example.digital_wallet.dto.response.BalanceResponse;
import com.example.digital_wallet.dto.response.WalletResponse;
import com.example.digital_wallet.entity.Wallet;

public final class WalletMapper {

    private WalletMapper() {}

    public static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getCreatedAt()
        );
    }

    public static BalanceResponse toBalance(Wallet wallet) {
        return new BalanceResponse(
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getCurrency()
        );
    }
}