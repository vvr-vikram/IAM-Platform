package com.enterprise.iam.controller;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.entity.AuditLog;
import com.enterprise.iam.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Management", description = "Administrative endpoints for managing users, roles, sessions and audit logging. Access is restricted.")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin:read') or hasRole('ADMIN')")
    @Operation(summary = "Get all users (Paginated)", description = "Requires authority 'admin:read' or role 'ADMIN'.")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('admin:write') or hasRole('ADMIN')")
    @Operation(summary = "Update user status (Enable/Lock)", description = "Allows admins to enable/disable or lock/unlock user accounts. Requires 'admin:write'.")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean enabled,
            @RequestParam boolean locked,
            Principal principal
    ) {
        adminService.updateUserStatus(userId, enabled, locked, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('admin:write') or hasRole('ADMIN')")
    @Operation(summary = "Assign roles to user", description = "Overwrites user roles with the provided list. Requires 'admin:write'.")
    public ResponseEntity<Void> assignRoles(
            @PathVariable Long userId,
            @RequestBody List<String> roles,
            Principal principal
    ) {
        adminService.assignRoles(userId, roles, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('audit:read') or hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "View security audit logs", description = "Fetches a historical log of all IAM events. Requires 'audit:read' or role 'MANAGER' / 'ADMIN'.")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(@PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('admin:read') or hasRole('ADMIN')")
    @Operation(summary = "List active user sessions", description = "Lists active refresh tokens in the system. Requires 'admin:read'.")
    public ResponseEntity<List<SessionResponse>> getActiveSessions() {
        return ResponseEntity.ok(adminService.getActiveSessions());
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    @PreAuthorize("hasAuthority('admin:write') or hasRole('ADMIN')")
    @Operation(summary = "Revoke user session", description = "Terminates the specified user session. Requires 'admin:write'.")
    public ResponseEntity<Void> revokeSession(@PathVariable Long sessionId, Principal principal) {
        adminService.revokeSession(sessionId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
