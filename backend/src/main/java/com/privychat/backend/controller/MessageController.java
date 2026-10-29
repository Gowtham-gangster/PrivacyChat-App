package com.privychat.backend.controller;

import com.privychat.backend.dto.chat.MessageReceiptDto;
import com.privychat.backend.dto.chat.MessageResponse;
import com.privychat.backend.dto.chat.SendMessageRequest;
import com.privychat.backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Messages", description = "Encrypted message submission, receipts, and offline queue retrieval")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Submit an end-to-end encrypted message")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(messageService.sendMessage(userId, req));
    }

    @PostMapping("/receipts")
    @Operation(summary = "Submit delivery or read receipts")
    public ResponseEntity<Void> submitReceipts(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody MessageReceiptDto dto) {
        messageService.markReceipts(userId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/offline")
    @Operation(summary = "Retrieve pending offline messages after reconnection")
    public ResponseEntity<List<MessageResponse>> getOfflineMessages(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(messageService.getOfflineMessages(userId));
    }
}

