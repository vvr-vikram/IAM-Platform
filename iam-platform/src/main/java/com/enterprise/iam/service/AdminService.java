package com.enterprise.iam.service;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.*;
import com.enterprise.iam.exception.IamException;
import com.enterprise.iam.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository,
                        AuditLogRepository auditLogRepository, RefreshTokenRepository refreshTokenRepository,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .mfaEnabled(user.isMfaEnabled())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .locked(user.isLocked())
                .build());
    }

    @Transactional
    public void updateUserStatus(Long userId, boolean enabled, boolean locked, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));
        
        user.setEnabled(enabled);
        user.setLocked(locked);
        if (!locked) {
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
        }
        userRepository.save(user);

        auditLogService.log(user.getId(), user.getUsername(), "ADMIN_UPDATE_USER_STATUS", "SUCCESS",
                String.format("Updated by admin '%s': enabled=%b, locked=%b", adminUsername, enabled, locked));
    }

    @Transactional
    public void assignRoles(Long userId, List<String> roleNames, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IamException("User not found", HttpStatus.NOT_FOUND));

        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IamException("Role not found: " + name, HttpStatus.BAD_REQUEST)))
                .collect(Collectors.toSet());

        user.setRoles(roles);
        userRepository.save(user);

        auditLogService.log(user.getId(), user.getUsername(), "ADMIN_ASSIGN_ROLES", "SUCCESS",
                String.format("Assigned roles by admin '%s': %s", adminUsername, roleNames));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions() {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> !token.isRevoked() && !token.isExpired())
                .map(token -> new SessionResponse(
                        token.getId(),
                        token.getUser().getUsername(),
                        token.getExpiryDate().toString()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long sessionId, String adminUsername) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new IamException("Session not found", HttpStatus.NOT_FOUND));
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        auditLogService.log(token.getUser().getId(), token.getUser().getUsername(), "ADMIN_REVOKE_SESSION", "SUCCESS",
                String.format("Session id %d revoked by admin '%s'", sessionId, adminUsername));
    }
}
