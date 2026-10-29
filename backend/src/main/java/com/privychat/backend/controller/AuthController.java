package com.privychat.backend.controller;

import com.privychat.backend.dto.auth.AuthResponse;
import com.privychat.backend.dto.auth.LoginRequest;
import com.privychat.backend.dto.auth.OtpVerificationRequest;
import com.privychat.backend.dto.auth.RefreshTokenRequest;
import com.privychat.backend.dto.auth.RegisterRequest;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, OTP verification, and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new account")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserProfileResponse response = authService.register(req);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify email ownership with 6-digit OTP")
    public ResponseEntity<Map<String, Boolean>> verifyEmailOtp(@Valid @RequestBody OtpVerificationRequest req) {
        boolean verified = authService.verifyEmailOtp(req.getIdentifier(), req.getOtpCode());
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PostMapping("/verify-phone-otp")
    @Operation(summary = "Verify phone number ownership with 6-digit OTP")
    public ResponseEntity<Map<String, Boolean>> verifyPhoneOtp(@Valid @RequestBody OtpVerificationRequest req) {
        boolean verified = authService.verifyPhoneOtp(req.getIdentifier(), req.getOtpCode());
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT access and refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        AuthResponse response = authService.refresh(req.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout active device session")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID userId, @RequestParam(required = false) UUID deviceId) {
        if (userId != null) {
            authService.logout(userId, deviceId);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-sessions")
    @Operation(summary = "Revoke all active device sessions for current user")
    public ResponseEntity<Void> revokeAllSessions(@AuthenticationPrincipal UUID userId) {
        authService.revokeAllSessions(userId);
        return ResponseEntity.noContent().build();
    }
}

