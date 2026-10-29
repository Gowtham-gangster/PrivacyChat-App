package com.privychat.backend.controller;

import com.privychat.backend.dto.user.ContactLookupRequest;
import com.privychat.backend.dto.user.ContactResponse;
import com.privychat.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Privacy-preserving contact matching and discovery")
public class ContactController {

    private final UserService userService;

    public ContactController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/discover")
    @Operation(summary = "Match contacts to registered users by phone numbers")
    public ResponseEntity<List<ContactResponse>> discoverContacts(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ContactLookupRequest req) {
        return ResponseEntity.ok(userService.discoverContacts(userId, req.getPhoneNumbers()));
    }
}

