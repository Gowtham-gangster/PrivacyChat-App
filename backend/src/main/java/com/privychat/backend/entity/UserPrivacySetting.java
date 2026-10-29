package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_privacy_settings")
public class UserPrivacySetting {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "read_receipts_enabled", nullable = false)
    private boolean readReceiptsEnabled = true;

    @Column(name = "last_seen_visibility", nullable = false, length = 32)
    private String lastSeenVisibility = "EVERYONE"; // EVERYONE, CONTACTS, NOBODY

    @Column(name = "profile_photo_visibility", nullable = false, length = 32)
    private String profilePhotoVisibility = "EVERYONE"; // EVERYONE, CONTACTS, NOBODY

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserPrivacySetting() {}

    public UserPrivacySetting(User user) {
        this.user = user;
        this.userId = user.getId();
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isReadReceiptsEnabled() { return readReceiptsEnabled; }
    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) { this.readReceiptsEnabled = readReceiptsEnabled; }

    public String getLastSeenVisibility() { return lastSeenVisibility; }
    public void setLastSeenVisibility(String lastSeenVisibility) { this.lastSeenVisibility = lastSeenVisibility; }

    public String getProfilePhotoVisibility() { return profilePhotoVisibility; }
    public void setProfilePhotoVisibility(String profilePhotoVisibility) { this.profilePhotoVisibility = profilePhotoVisibility; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

