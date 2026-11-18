
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
import com.example.digital_wallet.repository.UserRepository;
import com.example.digital_wallet.repository.WalletRepository;
import com.example.digital_wallet.security.CustomUserDetails;
import com.example.digital_wallet.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest validRegister;

    @BeforeEach
    void setUp() {
        validRegister = new RegisterRequest(
                "Fiza Khan",
                "fiza@example.com",
                "9876543210",
                "MyPass123"
        );
    }

    // ---------- register ----------

    @Test
    @DisplayName("register: creates user + wallet when email and phone are unique")
    void register_success() {
        when(userRepository.existsByEmail("fiza@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("MyPass123")).thenReturn("HASHED_PW");

        User savedUser = User.builder()
                .id(1L)
                .fullName("Fiza Khan")
                .email("fiza@example.com")
                .phoneNumber("9876543210")
                .password("HASHED_PW")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(validRegister);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("fiza@example.com");
        assertThat(response.roles()).containsExactly("USER");

        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("register: throws EmailAlreadyExistsException on duplicate email")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("fiza@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegister))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("fiza@example.com");

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws InvalidOperationException on duplicate phone")
    void register_duplicatePhone_throws() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegister))
                .isInstanceOf(InvalidOperationException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    @DisplayName("login: returns token when credentials are valid")
    void login_success() {
        User user = User.builder()
                .id(1L)
                .email("fiza@example.com")
                .password("HASHED_PW")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(principal)).thenReturn("JWT-TOKEN");
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(new LoginRequest("fiza@example.com", "MyPass123"));

        assertThat(response.token()).isEqualTo("JWT-TOKEN");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(86400000L);
        assertThat(response.user().email()).isEqualTo("fiza@example.com");
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException from AuthenticationManager")
    void login_wrongPassword_throws() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("fiza@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}