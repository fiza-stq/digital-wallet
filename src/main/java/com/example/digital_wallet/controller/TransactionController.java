

package com.example.digital_wallet.controller;

import com.example.digital_wallet.dto.response.PageResponse;
import com.example.digital_wallet.dto.response.TransactionResponse;
import com.example.digital_wallet.enums.TransactionStatus;
import com.example.digital_wallet.enums.TransactionType;
import com.example.digital_wallet.security.CurrentUser;
import com.example.digital_wallet.service.TransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<TransactionResponse>> myTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        // Sanitize page/size
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return ResponseEntity.ok(
                transactionService.getMyTransactions(
                        CurrentUser.get(), type, status, from, to, pageable));
    }
}