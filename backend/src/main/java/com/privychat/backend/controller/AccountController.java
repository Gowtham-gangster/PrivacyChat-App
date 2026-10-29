package com.privychat.backend.controller;

import com.privychat.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account Management", description = "Data export and account deletion")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/export")
    @Operation(summary = "Export account profile and settings data")
    public ResponseEntity<Map<String, Object>> exportData(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.exportUserData(userId));
    }

    @DeleteMapping
    @Operation(summary = "Permanently delete account with password confirmation")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String password) {
        userService.deleteAccount(userId, password);
        return ResponseEntity.noContent().build();
    }
}

