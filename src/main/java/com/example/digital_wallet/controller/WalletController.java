package com.example.digital_wallet.controller;

import com.example.digital_wallet.dto.request.TopUpRequest;
import com.example.digital_wallet.dto.request.TransferRequest;
import com.example.digital_wallet.dto.response.BalanceResponse;
import com.example.digital_wallet.dto.response.TransactionResponse;
import com.example.digital_wallet.dto.response.WalletResponse;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.security.CurrentUser;
import com.example.digital_wallet.service.TransferService;
import com.example.digital_wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private final TransferService transferService;

    public WalletController(WalletService walletService,
                            TransferService transferService) {
        this.walletService = walletService;
        this.transferService = transferService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet() {
        return ResponseEntity.ok(walletService.getMyWallet(CurrentUser.get()));
    }

    @GetMapping("/me/balance")
    public ResponseEntity<BalanceResponse> getMyBalance() {
        return ResponseEntity.ok(walletService.getMyBalance(CurrentUser.get()));
    }

    @PostMapping("/me/topup")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(CurrentUser.get(), request.amount()));
    }

    @PostMapping("/me/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(CurrentUser.get(), request));
    }
}