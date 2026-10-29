package com.privychat.backend.controller;

import com.privychat.backend.dto.chat.ConversationResponse;
import com.privychat.backend.dto.chat.DirectConversationRequest;
import com.privychat.backend.dto.chat.MessageResponse;
import com.privychat.backend.service.ConversationService;
import com.privychat.backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "One-to-one conversation management and history")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "List all active one-to-one conversations for the current user")
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @PostMapping("/direct")
    @Operation(summary = "Get or create a 1-to-1 conversation with a contact")
    public ResponseEntity<ConversationResponse> getOrCreateConversation(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DirectConversationRequest req) {
        return ResponseEntity.ok(conversationService.getOrCreateDirectConversation(userId, req.getRecipientUserId()));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get paged message history for a conversation")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(messageService.getConversationMessages(conversationId, userId, before, limit));
    }
}

