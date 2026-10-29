package com.privychat.backend.controller;

import com.privychat.backend.dto.user.PrivacySettingsDto;
import com.privychat.backend.dto.user.UpdateProfileRequest;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "User Profile", description = "Profile inspection, profile editing, and privacy settings")
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

    @PutMapping
    @Operation(summary = "Update display name or avatar")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(userId, req));
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

