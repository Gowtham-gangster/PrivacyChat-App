package com.privychat.backend.dto.chat;

import com.privychat.backend.dto.user.UserProfileResponse;

import java.time.Instant;
import java.util.UUID;

public class ConversationResponse {

    private UUID id;
    private UserProfileResponse participant;
    private Instant lastMessageTimestamp;
    private long unreadCount;
    private Instant createdAt;

    public ConversationResponse() {}

    public ConversationResponse(UUID id, UserProfileResponse participant, Instant lastMessageTimestamp, long unreadCount, Instant createdAt) {
        this.id = id;
        this.participant = participant;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserProfileResponse getParticipant() { return participant; }
    public void setParticipant(UserProfileResponse participant) { this.participant = participant; }

    public Instant getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Instant lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

