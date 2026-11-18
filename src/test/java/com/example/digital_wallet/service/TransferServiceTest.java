

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
import com.example.digital_wallet.repository.TransactionRepository;
import com.example.digital_wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private TransferService transferService;

    private User sender;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(1L).email("fiza@example.com").build();

        senderWallet = Wallet.builder()
                .id(10L).walletNumber("wallet-fiza").user(sender)
                .balance(new BigDecimal("500.0000"))
                .currency("INR").status(WalletStatus.ACTIVE)
                .build();

        receiverWallet = Wallet.builder()
                .id(20L).walletNumber("wallet-bob")
                .balance(new BigDecimal("100.0000"))
                .currency("INR").status(WalletStatus.ACTIVE)
                .build();
    }

    private TransferRequest req(String receiver, String amount, String key) {
        return new TransferRequest(receiver, new BigDecimal(amount), "test", key);
    }

    @Test
    @DisplayName("transfer: moves money between wallets and records transaction")
    void transfer_success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("wallet-bob")).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.findByIdempotencyKey(null)).thenReturn(Optional.empty());
        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiverWallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(100L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        TransactionResponse response = transferService.transfer(sender, req("wallet-bob", "250", null));

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("250.0000");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("350.0000");
        assertThat(response.type()).isEqualTo("TRANSFER");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.amount()).isEqualByComparingTo("250");
    }

    @Test
    @DisplayName("transfer: throws InsufficientBalanceException when balance too low")
    void transfer_insufficientBalance_throws() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("wallet-bob")).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.findByIdempotencyKey(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transferService.transfer(sender, req("wallet-bob", "99999", null)))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: throws when sender tries to transfer to self")
    void transfer_toSelf_throws() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("wallet-fiza")).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findByIdempotencyKey(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transferService.transfer(sender, req("wallet-fiza", "10", null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("own wallet");
    }

    @Test
    @DisplayName("transfer: returns existing transaction when idempotency key is reused")
    void transfer_idempotent_replaysPreviousTransaction() {
        Transaction existing = Transaction.builder()
                .id(99L).reference("TXN-EXISTING")
                .senderWallet(senderWallet).receiverWallet(receiverWallet)
                .amount(new BigDecimal("250"))
                .currency("INR")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("key-abc")
                .createdAt(LocalDateTime.now())
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findByIdempotencyKey("key-abc")).thenReturn(Optional.of(existing));

        TransactionResponse response = transferService.transfer(
                sender, req("wallet-bob", "250", "key-abc"));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.reference()).isEqualTo("TXN-EXISTING");

        // Crucially: no wallet changes occurred
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: throws when receiver wallet doesn't exist")
    void transfer_receiverNotFound_throws() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(transactionRepository.findByIdempotencyKey(null)).thenReturn(Optional.empty());
        when(walletRepository.findByWalletNumber("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transferService.transfer(sender, req("unknown", "10", null)))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    @DisplayName("transfer: throws when sender wallet is FROZEN")
    void transfer_senderWalletFrozen_throws() {
        senderWallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("wallet-bob")).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.findByIdempotencyKey(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transferService.transfer(sender, req("wallet-bob", "10", null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not active");
    }
}