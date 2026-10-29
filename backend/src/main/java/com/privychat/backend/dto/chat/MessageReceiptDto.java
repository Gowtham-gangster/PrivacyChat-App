package com.privychat.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public class MessageReceiptDto {

    @NotEmpty(message = "Message IDs cannot be empty")
    private List<UUID> messageIds;

    @NotBlank(message = "Receipt type is required")
    private String type; // DELIVERED, READ

    private UUID conversationId;

    public MessageReceiptDto() {}

    public MessageReceiptDto(List<UUID> messageIds, String type, UUID conversationId) {
        this.messageIds = messageIds;
        this.type = type;
        this.conversationId = conversationId;
    }

    public List<UUID> getMessageIds() { return messageIds; }
    public void setMessageIds(List<UUID> messageIds) { this.messageIds = messageIds; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
}

