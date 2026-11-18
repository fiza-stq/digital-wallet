
package com.example.digital_wallet.controller;

import com.example.digital_wallet.dto.request.LoginRequest;
import com.example.digital_wallet.dto.request.RegisterRequest;
import com.example.digital_wallet.dto.response.AuthResponse;
import com.example.digital_wallet.dto.response.UserResponse;
import com.example.digital_wallet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class cAuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);   // 201
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);   // 200
    }
}
