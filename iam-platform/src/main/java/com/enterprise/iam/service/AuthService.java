package com.enterprise.iam.service;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.RefreshToken;
import com.enterprise.iam.entity.Role;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.exception.IamException;
import com.enterprise.iam.repository.RefreshTokenRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final CacheService cacheService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TotpService totpService, EmailService emailService,
                       AuditLogService auditLogService, CacheService cacheService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.cacheService = cacheService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            auditLogService.log(null, request.getUsername(), "REGISTER", "FAILURE", "Username already taken");
            throw new IamException("Username is already taken", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            auditLogService.log(null, request.getUsername(), "REGISTER", "FAILURE", "Email already taken");
            throw new IamException("Email is already taken", HttpStatus.BAD_REQUEST);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IamException("Default Role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false) // Disabled until email is verified
                .emailVerified(false)
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        // Generate email verification OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        cacheService.put("email_verify:" + user.getEmail(), otp, Duration.ofMinutes(15));
        
        emailService.sendVerificationEmail(user.getEmail(), otp);
        auditLogService.log(user.getId(), user.getUsername(), "REGISTER", "SUCCESS", "User registered. Verification OTP sent.");

        return "Registration successful. Please check your email for the verification code.";
    }

    @Transactional
    public String verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IamException("User not found with email: " + request.getEmail(), HttpStatus.NOT_FOUND));

        if (user.isEmailVerified()) {
            return "Email is already verified.";
        }

        String cachedOtp = (String) cacheService.get("email_verify:" + request.getEmail());
        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            auditLogService.log(user.getId(), user.getUsername(), "VERIFY_EMAIL", "FAILURE", "Invalid or expired OTP");
            throw new IamException("Invalid or expired verification code", HttpStatus.BAD_REQUEST);
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        cacheService.remove("email_verify:" + request.getEmail());
        auditLogService.log(user.getId(), user.getUsername(), "VERIFY_EMAIL", "SUCCESS", "Email verified successfully");

        return "Email verified successfully. You can now login.";
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        // Check account lock status
        if (user.isLocked()) {
            if (user.getLockTime().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                user.setLocked(false);
                user.setLockTime(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                log.info("User {} account unlocked automatically.", user.getUsername());
            } else {
                auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "FAILURE", "Account is locked");
                throw new LockedException("Your account is locked due to too many failed attempts. Try again later.");
            }
        }

        if (!user.isEnabled()) {
            auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "FAILURE", "Account is disabled/unverified");
            throw new DisabledException("Account is disabled. Please verify your email first.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Login Success: reset attempts
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        // Check if MFA is required
        if (user.isMfaEnabled()) {
            String mfaToken = UUID.randomUUID().toString();
            // Store temporary MFA session in cache (valid for 5 mins)
            cacheService.put("mfa_session:" + mfaToken, user.getUsername(), Duration.ofMinutes(5));
            
            auditLogService.log(user.getId(), user.getUsername(), "LOGIN_INIT", "SUCCESS", "MFA code requested");
            return LoginResponse.builder()
                    .mfaRequired(true)
                    .mfaToken(mfaToken)
                    .build();
        }

        // Generate Access and Refresh tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "SUCCESS", "User logged in");
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public LoginResponse verifyMfaLogin(MfaVerifyRequest request) {
        String username = (String) cacheService.get("mfa_session:" + request.getMfaToken());
        if (username == null) {
            throw new IamException("MFA session expired or invalid token", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        boolean isValid = totpService.verifyCode(user.getMfaSecret(), request.getCode());
        if (!isValid) {
            auditLogService.log(user.getId(), user.getUsername(), "MFA_VERIFY", "FAILURE", "Invalid TOTP code");
            throw new IamException("Invalid MFA code", HttpStatus.BAD_REQUEST);
        }

        // Generate JWTs
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        cacheService.remove("mfa_session:" + request.getMfaToken());
        auditLogService.log(user.getId(), user.getUsername(), "MFA_VERIFY", "SUCCESS", "MFA authentication successful");

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IamException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new IamException("Refresh token is expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        
        auditLogService.log(user.getId(), user.getUsername(), "TOKEN_REFRESH", "SUCCESS", "Access token refreshed");
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .mfaRequired(false)
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public void logout(String authHeader, String refreshTokenStr) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            jwtService.blacklistToken(accessToken);
        }

        if (refreshTokenStr != null) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                auditLogService.log(token.getUser().getId(), token.getUser().getUsername(), "LOGOUT", "SUCCESS", "User logged out");
            });
        }
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IamException("User not found with email: " + request.getEmail(), HttpStatus.NOT_FOUND));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        cacheService.put("password_reset:" + user.getEmail(), otp, Duration.ofMinutes(15));
        
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        auditLogService.log(user.getId(), user.getUsername(), "FORGOT_PASSWORD", "SUCCESS", "Password reset link/OTP sent");

        return "Password reset code sent to your email.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IamException("User not found with email: " + request.getEmail(), HttpStatus.NOT_FOUND));

        String cachedOtp = (String) cacheService.get("password_reset:" + request.getEmail());
        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            auditLogService.log(user.getId(), user.getUsername(), "RESET_PASSWORD", "FAILURE", "Invalid or expired OTP");
            throw new IamException("Invalid or expired reset code", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        cacheService.remove("password_reset:" + request.getEmail());
        auditLogService.log(user.getId(), user.getUsername(), "RESET_PASSWORD", "SUCCESS", "Password reset successfully");

        return "Password has been reset successfully. You can now login.";
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            user.setLockTime(LocalDateTime.now());
            auditLogService.log(user.getId(), user.getUsername(), "LOCKOUT", "SUCCESS", "Account locked due to consecutive failures");
            log.warn("User {} account has been locked due to 5 consecutive failed login attempts.", user.getUsername());
        }

        userRepository.save(user);
        auditLogService.log(user.getId(), user.getUsername(), "LOGIN", "FAILURE", "Failed login attempt: " + attempts);
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);

        String tokenStr = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiryDate(Instant.now().plusMillis(604800000)) // 7 days
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }
}
