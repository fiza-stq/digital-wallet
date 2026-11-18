
package com.example.digital_wallet.repository;

import com.example.digital_wallet.entity.Transaction;
import com.example.digital_wallet.enums.TransactionStatus;
import com.example.digital_wallet.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // Transaction history for a wallet — sender OR receiver
    @Query("""
           SELECT t FROM Transaction t
           WHERE t.senderWallet.id = :walletId
              OR t.receiverWallet.id = :walletId
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByWalletId(@Param("walletId") Long walletId, Pageable pageable);

    // Filter by type
    @Query("""
           SELECT t FROM Transaction t
           WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
             AND t.type = :type
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByWalletIdAndType(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            Pageable pageable);

    // Filter by status
    @Query("""
           SELECT t FROM Transaction t
           WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
             AND t.status = :status
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByWalletIdAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    // Filter by date range
    @Query("""
           SELECT t FROM Transaction t
           WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
             AND t.createdAt BETWEEN :from AND :to
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    // Filter by BOTH type and status
    @Query("""
           SELECT t FROM Transaction t
           WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
             AND t.type = :type
             AND t.status = :status
           ORDER BY t.createdAt DESC
           """)
    Page<Transaction> findByWalletIdAndTypeAndStatus(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable);
}

