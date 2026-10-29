package com.privychat.backend.dto.user;

import java.util.UUID;

public class UserProfileResponse {

    private UUID id;
    private String displayName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private boolean emailVerified;
    private boolean phoneVerified;

    public UserProfileResponse() {}

    public UserProfileResponse(UUID id, String displayName, String email, String phoneNumber, String avatarUrl, boolean emailVerified, boolean phoneVerified) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
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
}

