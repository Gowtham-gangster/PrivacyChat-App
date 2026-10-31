package com.privychat.backend.controller;

import com.privychat.backend.dto.auth.*;
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
@Tag(name = "Authentication", description = "Registration, login, OTP email verification, password reset, and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new account and dispatch verification email via SMTP")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserProfileResponse response = authService.register(req);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify email ownership with 6-digit OTP received via SMTP")
    public ResponseEntity<Map<String, Boolean>> verifyEmailOtp(@Valid @RequestBody OtpVerificationRequest req) {
        boolean verified = authService.verifyEmailOtp(req.getIdentifier(), req.getOtpCode());
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend registration email verification code via SMTP")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        authService.resendRegistrationOtp(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "If an account exists, a new verification code has been dispatched."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset code sent exclusively to registered email via SMTP")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        String msg = authService.forgotPassword(req);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP received via SMTP")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password successfully reset. You can now log in with your new password."));
    }

    @PostMapping("/verify-phone-otp")
    @Operation(summary = "Discontinued: phone verification via SMS is removed in favor of SMTP email verification")
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
