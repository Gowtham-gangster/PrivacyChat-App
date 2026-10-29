package com.privychat.backend.dto.user;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;

    private String avatarUrl;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

