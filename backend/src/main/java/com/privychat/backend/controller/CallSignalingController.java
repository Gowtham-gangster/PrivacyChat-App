package com.privychat.backend.controller;

import com.privychat.backend.dto.call.CallSessionResponse;
import com.privychat.backend.dto.call.CallSignalMessage;
import com.privychat.backend.dto.chat.MessageReceiptDto;
import com.privychat.backend.dto.chat.SendMessageRequest;
import com.privychat.backend.service.CallSignalingService;
import com.privychat.backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@Tag(name = "Voice & Video Calls", description = "WebRTC call signaling and call history")
public class CallSignalingController {

    private final CallSignalingService callSignalingService;
    private final MessageService messageService;

    public CallSignalingController(CallSignalingService callSignalingService, MessageService messageService) {
        this.callSignalingService = callSignalingService;
        this.messageService = messageService;
    }

    // =========================================================================
    // REST Endpoints
    // =========================================================================

    @PostMapping("/api/v1/calls/signal")
    @ResponseBody
    @Operation(summary = "Send a WebRTC call signaling message (REST fallback)")
    public ResponseEntity<Void> sendSignalRest(
            @AuthenticationPrincipal UUID userId,
            @RequestBody CallSignalMessage signal) {
        callSignalingService.handleSignal(userId, signal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/calls/history")
    @ResponseBody
    @Operation(summary = "Get user call history")
    public ResponseEntity<List<CallSessionResponse>> getCallHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(callSignalingService.getCallHistory(userId, limit));
    }

    // =========================================================================
    // STOMP WebSocket Message Handlers
    // =========================================================================

    @MessageMapping("/call.signal")
    public void handleCallSignalWebSocket(Principal principal, @Payload CallSignalMessage signal) {
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            callSignalingService.handleSignal(userId, signal);
        }
    }

    @MessageMapping("/chat.send")
    public void handleChatMessageWebSocket(Principal principal, @Payload SendMessageRequest request) {
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            messageService.sendMessage(userId, request);
        }
    }

    @MessageMapping("/chat.receipt")
    public void handleChatReceiptWebSocket(Principal principal, @Payload MessageReceiptDto dto) {
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            messageService.markReceipts(userId, dto);
        }
    }
}

