
package com.example.digital_wallet.service;

import com.example.digital_wallet.config.JwtProperties;
import com.example.digital_wallet.dto.request.LoginRequest;
import com.example.digital_wallet.dto.request.RegisterRequest;
import com.example.digital_wallet.dto.response.AuthResponse;
import com.example.digital_wallet.dto.response.UserResponse;
import com.example.digital_wallet.entity.User;
import com.example.digital_wallet.entity.Wallet;
import com.example.digital_wallet.enums.Role;
import com.example.digital_wallet.exception.EmailAlreadyExistsException;
import com.example.digital_wallet.exception.InvalidOperationException;
import com.example.digital_wallet.mapper.UserMapper;
import com.example.digital_wallet.repository.UserRepository;
import com.example.digital_wallet.repository.WalletRepository;
import com.example.digital_wallet.security.CustomUserDetails;
import com.example.digital_wallet.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Register a new user + auto-create their wallet.
     * Runs in a single transaction: if wallet creation fails, user is rolled back too.
     */
    @Transactional
    public UserResponse register(RegisterRequest req) {
        // 1. Uniqueness checks
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + req.email());
        }
        if (userRepository.existsByPhoneNumber(req.phoneNumber())) {
            throw new InvalidOperationException("Phone number already registered");
        }

        // 2. Build user entity with BCrypt-hashed password
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email().toLowerCase().trim())
                .phoneNumber(req.phoneNumber())
                .password(passwordEncoder.encode(req.password()))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        // 3. Save user first (needed for wallet FK)
        User savedUser = userRepository.save(user);

        // 4. Create an empty wallet for the user
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .build();
        walletRepository.save(wallet);

        return UserMapper.toResponse(savedUser);
    }

    /**
     * Login: verify credentials, return JWT.
     */
    public AuthResponse login(LoginRequest req) {
        // Delegate authentication to Spring Security (uses our DaoAuthenticationProvider)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getUser();

        String token = jwtService.generateToken(principal);

        return new AuthResponse(
                token,
                "Bearer",
                jwtProperties.getExpirationMs(),
                UserMapper.toResponse(user)
        );
    }
}