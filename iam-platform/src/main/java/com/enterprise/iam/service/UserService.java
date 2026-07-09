package com.enterprise.iam.service;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.User;
import com.enterprise.iam.exception.IamException;
import com.enterprise.iam.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final CacheService cacheService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TotpService totpService, CacheService cacheService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.cacheService = cacheService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .mfaEnabled(user.isMfaEnabled())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .locked(user.isLocked())
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            auditLogService.log(user.getId(), username, "CHANGE_PASSWORD", "FAILURE", "Invalid current password");
            throw new IamException("Invalid current password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        auditLogService.log(user.getId(), username, "CHANGE_PASSWORD", "SUCCESS", "Password changed successfully");
    }

    @Transactional
    public MfaSetupResponse setupMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        String secret = totpService.generateSecret();
        String qrCodeUrl = totpService.getQrCodeUrl(secret, username);

        cacheService.put("mfa_setup:" + username, secret, Duration.ofMinutes(15));
        
        auditLogService.log(user.getId(), username, "MFA_SETUP", "SUCCESS", "MFA setup initiated");
        return new MfaSetupResponse(secret, qrCodeUrl);
    }

    @Transactional
    public void enableMfa(String username, MfaEnableRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        String cachedSecret = (String) cacheService.get("mfa_setup:" + username);
        
        String secretToVerify = request.getSecret();
        if (cachedSecret != null && !cachedSecret.equals(request.getSecret())) {
            secretToVerify = cachedSecret;
        }

        boolean isValid = totpService.verifyCode(secretToVerify, request.getCode());
        if (!isValid) {
            auditLogService.log(user.getId(), username, "MFA_ENABLE", "FAILURE", "Invalid TOTP code provided");
            throw new IamException("Invalid MFA code. Verification failed.", HttpStatus.BAD_REQUEST);
        }

        user.setMfaSecret(secretToVerify);
        user.setMfaEnabled(true);
        userRepository.save(user);

        cacheService.remove("mfa_setup:" + username);
        auditLogService.log(user.getId(), username, "MFA_ENABLE", "SUCCESS", "MFA enabled successfully");
    }

    @Transactional
    public void disableMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        auditLogService.log(user.getId(), username, "MFA_DISABLE", "SUCCESS", "MFA disabled successfully");
    }
}
