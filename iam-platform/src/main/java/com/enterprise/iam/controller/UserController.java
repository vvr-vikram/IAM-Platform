package com.enterprise.iam.controller;

import com.enterprise.iam.dto.*;
import com.enterprise.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile", description = "Endpoints for checking and managing the authenticated user's profile and security settings")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile details of the currently authenticated user.")
    public ResponseEntity<UserProfileResponse> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @PostMapping("/me/password")
    @Operation(summary = "Change password", description = "Changes password for the currently authenticated user.")
    public ResponseEntity<Void> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/mfa/setup")
    @Operation(summary = "Setup MFA (TOTP)", description = "Generates a new TOTP secret key and return QR code parameters.")
    public ResponseEntity<MfaSetupResponse> setupMfa(Principal principal) {
        return ResponseEntity.ok(userService.setupMfa(principal.getName()));
    }

    @PostMapping("/me/mfa/enable")
    @Operation(summary = "Enable MFA (TOTP)", description = "Verifies a TOTP code against the temporary secret and enables MFA on the account.")
    public ResponseEntity<Void> enableMfa(Principal principal, @Valid @RequestBody MfaEnableRequest request) {
        userService.enableMfa(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/mfa/disable")
    @Operation(summary = "Disable MFA (TOTP)", description = "Disables multi-factor authentication on the user's account.")
    public ResponseEntity<Void> disableMfa(Principal principal) {
        userService.disableMfa(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
