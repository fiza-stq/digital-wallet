

package com.example.digital_wallet.service;

import com.example.digital_wallet.dto.request.TransferRequest;
import com.example.digital_wallet.dto.response.TransactionResponse;
import com.example.digital_wallet.entity.Transaction;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.entity.Wallet;
import com.example.digital_wallet.enums.TransactionStatus;
import com.example.digital_wallet.enums.TransactionType;
import com.example.digital_wallet.enums.WalletStatus;
import com.example.digital_wallet.exception.InsufficientBalanceException;
import com.example.digital_wallet.exception.InvalidOperationException;
import com.example.digital_wallet.exception.WalletNotFoundException;
import com.example.digital_wallet.mapper.TransactionMapper;
import com.example.digital_wallet.repository.TransactionRepository;
import com.example.digital_wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(WalletRepository walletRepository,
                           TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse transfer(User sender, TransferRequest req) {

        // ---------- 1. Load sender wallet ----------
        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));

        // ---------- 2. Idempotency check ----------
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                Transaction t = existing.get();
                // Only the original sender may re-fetch this transaction
                if (t.getSenderWallet() != null
                        && t.getSenderWallet().getId().equals(senderWallet.getId())) {
                    return TransactionMapper.toResponse(t);
                }
                throw new InvalidOperationException("Idempotency key already used by another transaction");
            }
        }

        // ---------- 3. Load receiver wallet ----------
        Wallet receiverWallet = walletRepository
                .findByWalletNumber(req.receiverWalletNumber())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Receiver wallet not found: " + req.receiverWalletNumber()));

        // ---------- 4. Validations ----------
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new InvalidOperationException("Cannot transfer to your own wallet");
        }
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidOperationException("Sender wallet is not active");
        }
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidOperationException("Receiver wallet is not active");
        }
        if (senderWallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + senderWallet.getBalance());
        }

        // ---------- 5. Lock both wallets in ascending ID order (deadlock prevention) ----------
        Long firstId  = Math.min(senderWallet.getId(), receiverWallet.getId());
        Long secondId = Math.max(senderWallet.getId(), receiverWallet.getId());

        Wallet firstLocked  = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet disappeared during transfer"));
        Wallet secondLocked = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet disappeared during transfer"));

        // Re-map locked instances back to sender/receiver
        if (firstLocked.getId().equals(senderWallet.getId())) {
            senderWallet = firstLocked;
            receiverWallet = secondLocked;
        } else {
            senderWallet = secondLocked;
            receiverWallet = firstLocked;
        }

        // Re-check balance AFTER locking (balance might have changed before we got the lock)
        if (senderWallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + senderWallet.getBalance());
        }

        // ---------- 6. Perform the transfer ----------
        senderWallet.setBalance(senderWallet.getBalance().subtract(req.amount()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(req.amount()));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // ---------- 7. Record transaction ----------
        Transaction txn = Transaction.builder()
                .reference("TXN-" + UUID.randomUUID().toString()
                        .substring(0, 12).toUpperCase())
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(req.amount())
                .currency(senderWallet.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(req.idempotencyKey())
                .description(req.description())
                .build();

        Transaction saved = transactionRepository.save(txn);
        return TransactionMapper.toResponse(saved);
    }


}
