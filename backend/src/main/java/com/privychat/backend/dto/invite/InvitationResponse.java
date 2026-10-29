package com.privychat.backend.dto.invite;

import java.time.Instant;
import java.util.UUID;

public class InvitationResponse {

    private UUID id;
    private String recipientEmail;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;

    public InvitationResponse() {}

    public InvitationResponse(UUID id, String recipientEmail, String status, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.recipientEmail = recipientEmail;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

