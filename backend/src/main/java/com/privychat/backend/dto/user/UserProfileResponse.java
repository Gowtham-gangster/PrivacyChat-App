package com.privychat.backend.dto.user;

import java.time.Instant;
import java.util.UUID;

public class UserProfileResponse {

    private UUID id;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String pendingEmail;
    private String pendingPhoneNumber;
    private Instant createdAt;

    public UserProfileResponse() {}

    public UserProfileResponse(UUID id, String displayName, String email, String phoneNumber, String avatarUrl,
                               boolean emailVerified, boolean phoneVerified) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
    }

    public UserProfileResponse(UUID id, String displayName, String email, String phoneNumber, String avatarUrl,
                               boolean emailVerified, boolean phoneVerified,
                               String pendingEmail, String pendingPhoneNumber, Instant createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.pendingEmail = pendingEmail;
        this.pendingPhoneNumber = pendingPhoneNumber;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getPendingEmail() { return pendingEmail; }
    public void setPendingEmail(String pendingEmail) { this.pendingEmail = pendingEmail; }

    public String getPendingPhoneNumber() { return pendingPhoneNumber; }
    public void setPendingPhoneNumber(String pendingPhoneNumber) { this.pendingPhoneNumber = pendingPhoneNumber; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
