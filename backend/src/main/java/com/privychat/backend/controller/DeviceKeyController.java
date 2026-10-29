package com.privychat.backend.controller;

import com.privychat.backend.dto.keys.PreKeyBundleResponse;
import com.privychat.backend.dto.keys.PublishKeysRequest;
import com.privychat.backend.service.DeviceKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/keys")
@Tag(name = "Cryptographic Keys", description = "X3DH prekey bundle publishing and retrieval for E2EE")
public class DeviceKeyController {

    private final DeviceKeyService deviceKeyService;

    public DeviceKeyController(DeviceKeyService deviceKeyService) {
        this.deviceKeyService = deviceKeyService;
    }

    @PostMapping("/bundle")
    @Operation(summary = "Publish or replenish device identity key, signed prekey, and one-time prekeys")
    public ResponseEntity<Void> publishKeys(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PublishKeysRequest req) {
        deviceKeyService.publishKeys(userId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bundle/{recipientUserId}")
    @Operation(summary = "Retrieve recipient prekey bundle for X3DH session initialization")
    public ResponseEntity<PreKeyBundleResponse> getPreKeyBundle(@PathVariable UUID recipientUserId) {
        return ResponseEntity.ok(deviceKeyService.getPreKeyBundle(recipientUserId));
    }
}

