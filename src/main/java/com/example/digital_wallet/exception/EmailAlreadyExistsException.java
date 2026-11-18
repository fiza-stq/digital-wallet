
package com.example.digital_wallet.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);   // 409
    }
}