package com.enterprise.iam.service;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.Role;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.exception.IamException;
import com.enterprise.iam.repository.RefreshTokenRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TotpService totpService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AuthService authService;



    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .permissions(Collections.emptySet())
                .build();

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@enterprise.com")
                .password("encoded_password")
                .enabled(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .locked(false)
                .roles(Collections.singleton(role))
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@enterprise.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@enterprise.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        String result = authService.register(request);

        assertNotNull(result);
        assertTrue(result.contains("successful"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(cacheService, times(1)).put(eq("email_verify:test@enterprise.com"), anyString(), any());
        verify(emailService, times(1)).sendVerificationEmail(eq("test@enterprise.com"), anyString());
    }

    @Test
    void register_UsernameConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@enterprise.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IamException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyEmail_Success() {
        user.setEmailVerified(false);
        user.setEnabled(false);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@enterprise.com");
        request.setOtp("123456");

        when(userRepository.findByEmail("test@enterprise.com")).thenReturn(Optional.of(user));
        when(cacheService.get("email_verify:test@enterprise.com")).thenReturn("123456");

        String result = authService.verifyEmail(request);

        assertEquals("Email verified successfully. You can now login.", result);
        assertTrue(user.isEmailVerified());
        assertTrue(user.isEnabled());
        verify(userRepository, times(1)).save(user);
        verify(cacheService, times(1)).remove("email_verify:test@enterprise.com");
    }

    @Test
    void verifyEmail_WrongOtp() {
        user.setEmailVerified(false);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("test@enterprise.com");
        request.setOtp("wrong_otp");

        when(userRepository.findByEmail("test@enterprise.com")).thenReturn(Optional.of(user));
        when(cacheService.get("email_verify:test@enterprise.com")).thenReturn("123456");

        assertThrows(IamException.class, () -> authService.verifyEmail(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access_token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertFalse(response.isMfaRequired());
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    void login_Failure_LockoutTrigger() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong_password");

        user.setFailedLoginAttempts(4); // 5th failure will trigger lockout

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertTrue(user.isLocked());
        assertNotNull(user.getLockTime());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_AccountLocked() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        user.setLocked(true);
        user.setLockTime(LocalDateTime.now()); // Lock is active

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(LockedException.class, () -> authService.login(request));
    }
}
