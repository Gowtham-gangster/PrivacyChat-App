package com.privychat.backend.dto.user;

public class PrivacySettingsDto {

    private boolean readReceiptsEnabled = true;
    private String lastSeenVisibility = "EVERYONE"; // EVERYONE, CONTACTS, NOBODY
    private String profilePhotoVisibility = "EVERYONE";

    public PrivacySettingsDto() {}

    public PrivacySettingsDto(boolean readReceiptsEnabled, String lastSeenVisibility, String profilePhotoVisibility) {
        this.readReceiptsEnabled = readReceiptsEnabled;
        this.lastSeenVisibility = lastSeenVisibility;
        this.profilePhotoVisibility = profilePhotoVisibility;
    }

    public boolean isReadReceiptsEnabled() { return readReceiptsEnabled; }
    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) { this.readReceiptsEnabled = readReceiptsEnabled; }

    public String getLastSeenVisibility() { return lastSeenVisibility; }
    public void setLastSeenVisibility(String lastSeenVisibility) { this.lastSeenVisibility = lastSeenVisibility; }

    public String getProfilePhotoVisibility() { return profilePhotoVisibility; }
    public void setProfilePhotoVisibility(String profilePhotoVisibility) { this.profilePhotoVisibility = profilePhotoVisibility; }
}

