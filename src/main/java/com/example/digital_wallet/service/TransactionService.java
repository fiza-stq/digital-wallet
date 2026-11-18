
package com.example.digital_wallet.service;

import com.example.digital_wallet.dto.response.PageResponse;
import com.example.digital_wallet.dto.response.TransactionResponse;
import com.example.digital_wallet.entity.Transaction;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.entity.Wallet;
import com.example.digital_wallet.enums.TransactionStatus;
import com.example.digital_wallet.enums.TransactionType;
import com.example.digital_wallet.exception.InvalidOperationException;
import com.example.digital_wallet.exception.WalletNotFoundException;
import com.example.digital_wallet.mapper.TransactionMapper;
import com.example.digital_wallet.repository.TransactionRepository;
import com.example.digital_wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(WalletRepository walletRepository,
                              TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getMyTransactions(
            User user,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        // Default date range: last 30 days
        LocalDateTime now = LocalDateTime.now();
        if (from == null && to == null) {
            from = now.minusDays(30);
            to = now;
        } else {
            if (from == null) from = now.minusYears(100);
            if (to   == null) to   = now;
        }

        if (from.isAfter(to)) {
            throw new InvalidOperationException("'from' date must be before 'to' date");
        }

        Page<Transaction> page;

        // Apply the right repository query based on which filters are present
        if (type != null && status != null) {
            // (type + status)
            page = transactionRepository.findByWalletIdAndTypeAndStatus(
                    wallet.getId(), type, status, pageable);
        } else if (type != null) {
            page = transactionRepository.findByWalletIdAndType(
                    wallet.getId(), type, pageable);
        } else if (status != null) {
            page = transactionRepository.findByWalletIdAndStatus(
                    wallet.getId(), status, pageable);
        } else {
            // date range only (default last 30 days)
            page = transactionRepository.findByWalletIdAndDateRange(
                    wallet.getId(), from, to, pageable);
        }

        return PageResponse.from(page, TransactionMapper::toResponse);
    }
}