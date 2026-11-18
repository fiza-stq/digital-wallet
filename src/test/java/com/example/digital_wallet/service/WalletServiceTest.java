

package com.example.digital_wallet.service;

import com.example.digital_wallet.dto.response.BalanceResponse;
import com.example.digital_wallet.dto.response.WalletResponse;
import com.example.digital_wallet.entity.Transaction;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.entity.Wallet;
import com.example.digital_wallet.enums.WalletStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("fiza@example.com")
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .walletNumber("wallet-abc")
                .user(user)
                .balance(new BigDecimal("100.0000"))
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getMyWallet: returns wallet data for existing user")
    void getMyWallet_success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getMyWallet(user);

        assertThat(response.walletNumber()).isEqualTo("wallet-abc");
        assertThat(response.balance()).isEqualByComparingTo("100.0000");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getMyWallet: throws WalletNotFoundException when user has no wallet")
    void getMyWallet_notFound() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getMyWallet(user))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    @DisplayName("getMyBalance: returns the current balance")
    void getMyBalance_success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        BalanceResponse response = walletService.getMyBalance(user);

        assertThat(response.balance()).isEqualByComparingTo("100.0000");
        assertThat(response.currency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("topUp: adds amount to balance and records transaction")
    void topUp_success() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.topUp(user, new BigDecimal("250.00"));

        assertThat(response.balance()).isEqualByComparingTo("350.0000");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("topUp: throws when amount is zero or negative")
    void topUp_invalidAmount_throws() {
        assertThatThrownBy(() -> walletService.topUp(user, new BigDecimal("0")))
                .isInstanceOf(InvalidOperationException.class);

        assertThatThrownBy(() -> walletService.topUp(user, new BigDecimal("-5")))
                .isInstanceOf(InvalidOperationException.class);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("topUp: rejects when wallet is not ACTIVE")
    void topUp_frozenWallet_throws() {
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.topUp(user, new BigDecimal("10")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not active");
    }
}