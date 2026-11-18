
package com.example.digital_wallet.service;

import com.example.digital_wallet.dto.response.BalanceResponse;
import com.example.digital_wallet.dto.response.WalletResponse;
import com.example.digital_wallet.entity.Transaction;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.entity.Wallet;
import com.example.digital_wallet.enums.TransactionStatus;
import com.example.digital_wallet.enums.TransactionType;
import com.example.digital_wallet.enums.WalletStatus;
import com.example.digital_wallet.exception.InvalidOperationException;
import com.example.digital_wallet.exception.WalletNotFoundException;
import com.example.digital_wallet.mapper.WalletMapper;
import com.example.digital_wallet.repository.TransactionRepository;
import com.example.digital_wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // ---------- Read operations ----------

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(User user) {
        Wallet wallet = loadWalletForUser(user);
        return WalletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getMyBalance(User user) {
        Wallet wallet = loadWalletForUser(user);
        return WalletMapper.toBalance(wallet);
    }

    // ---------- Write operations ----------

    /**
     * Simulated top-up. In real life this would be triggered by a payment gateway callback.
     * For our demo, the user asks for it and we add money.
     */
    @Transactional
    public WalletResponse topUp(User user, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOperationException("Top-up amount must be greater than 0");
        }

        Wallet wallet = loadWalletForUser(user);

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidOperationException("Wallet is not active: " + wallet.getStatus());
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Record the transaction for history
        Transaction txn = Transaction.builder()
                .reference("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .receiverWallet(wallet)          // money flows INTO the wallet
                .amount(amount)
                .currency(wallet.getCurrency())
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.SUCCESS)
                .description("Wallet top-up")
                .build();
        transactionRepository.save(txn);

        return WalletMapper.toResponse(wallet);
    }

    // ---------- Helpers ----------

    private Wallet loadWalletForUser(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Wallet not found for user: " + user.getEmail()));
    }
}