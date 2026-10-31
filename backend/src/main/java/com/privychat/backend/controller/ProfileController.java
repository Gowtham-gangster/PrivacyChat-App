package com.privychat.backend.controller;

import com.privychat.backend.dto.user.*;
import com.privychat.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "User Profile", description = "Profile inspection, profile editing, avatar storage, verification, and privacy settings")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user's profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping
    @Operation(summary = "Update display name")
    public ResponseEntity<UserProfileResponse> patchProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    @PutMapping
    @Operation(summary = "Update display name (compatibility)")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    @PostMapping("/email-change")
    @Operation(summary = "Request email change and dispatch verification code")
    public ResponseEntity<Map<String, String>> requestEmailChange(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody EmailChangeRequest req) {
        userService.requestEmailChange(userId, req);
        return ResponseEntity.ok(Map.of("message", "Verification code dispatched to " + req.getNewEmail().trim().toLowerCase()));
    }

    @PostMapping("/email-change/verify")
    @Operation(summary = "Verify code and commit email change")
    public ResponseEntity<UserProfileResponse> verifyEmailChange(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerifyEmailChangeRequest req) {
        return ResponseEntity.ok(userService.verifyEmailChange(userId, req));
    }

    @PostMapping("/phone-change")
    @Operation(summary = "Request phone change and dispatch verification code to verified email via SMTP")
    public ResponseEntity<Map<String, String>> requestPhoneChange(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PhoneChangeRequest req) {
        userService.requestPhoneChange(userId, req);
        return ResponseEntity.ok(Map.of("message", "A verification code has been sent to your registered email address."));
    }

    @PostMapping("/phone-change/verify")
    @Operation(summary = "Verify OTP and commit phone number change")
    public ResponseEntity<UserProfileResponse> verifyPhoneChange(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody VerifyPhoneChangeRequest req) {
        return ResponseEntity.ok(userService.verifyPhoneChange(userId, req));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace avatar image in private Supabase Storage")
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("file") MultipartFile file) throws IOException, InterruptedException {
        return ResponseEntity.ok(userService.uploadAvatar(userId, file));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remove avatar image and restore default avatar")
    public ResponseEntity<UserProfileResponse> deleteAvatar(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.deleteAvatar(userId));
    }

    @GetMapping("/avatar/{targetUserId}")
    @Operation(summary = "Stream avatar image for a user, enforcing privacy rules")
    public ResponseEntity<byte[]> getAvatar(
            @AuthenticationPrincipal UUID requesterId,
            @PathVariable UUID targetUserId) throws IOException, InterruptedException {
        byte[] bytes = userService.getAvatarBytes(requesterId, targetUserId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(bytes);
    }

    @GetMapping("/privacy")
    @Operation(summary = "Get privacy controls")
    public ResponseEntity<PrivacySettingsDto> getPrivacySettings(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getPrivacySettings(userId));
    }

    @PutMapping("/privacy")
    @Operation(summary = "Update privacy controls (read receipts, visibility)")
    public ResponseEntity<PrivacySettingsDto> updatePrivacySettings(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PrivacySettingsDto dto) {
        return ResponseEntity.ok(userService.updatePrivacySettings(userId, dto));
    }
}
