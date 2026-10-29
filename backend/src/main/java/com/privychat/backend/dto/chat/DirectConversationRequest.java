package com.privychat.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class DirectConversationRequest {

    @NotNull(message = "Recipient user ID is required")
    private UUID recipientUserId;

    public DirectConversationRequest() {}

    public DirectConversationRequest(UUID recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }
}

