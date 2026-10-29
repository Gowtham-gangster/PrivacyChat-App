package com.privychat.backend.controller;

import com.privychat.backend.dto.invite.InvitationResponse;
import com.privychat.backend.dto.invite.SendInvitationRequest;
import com.privychat.backend.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@Tag(name = "Friend Invitations", description = "Send email invitations with opaque tokens to non-registered friends")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send an invitation email to a friend")
    public ResponseEntity<InvitationResponse> sendInvitation(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SendInvitationRequest req) {
        return ResponseEntity.ok(invitationService.sendInvitation(userId, req));
    }
}

