
package com.example.digital_wallet.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors   // null unless validation failed
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse ofValidation(int status, String error, String message,
                                                String path, Map<String, String> fields) {
        return new ApiErrorResponse(LocalDateTime.now(), status, error, message, path, fields);
    }
}
