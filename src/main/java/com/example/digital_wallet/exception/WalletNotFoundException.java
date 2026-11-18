
package com.example.digital_wallet.exception;

import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends ApiException {
    public WalletNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}