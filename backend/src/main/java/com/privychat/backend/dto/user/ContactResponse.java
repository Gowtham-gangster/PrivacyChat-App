package com.privychat.backend.dto.user;

import java.util.UUID;

public class ContactResponse {

    private UUID userId;
    private String displayName;
    private String phoneNumber;
    private String avatarUrl;
    private boolean registered;

    public ContactResponse() {}

    public ContactResponse(UUID userId, String displayName, String phoneNumber, String avatarUrl, boolean registered) {
        this.userId = userId;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.registered = registered;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isRegistered() { return registered; }
    public void setRegistered(boolean registered) { this.registered = registered; }
}

