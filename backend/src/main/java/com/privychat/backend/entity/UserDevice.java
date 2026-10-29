package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_device_reg", columnNames = {"user_id", "registration_id"})
})
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_label", nullable = false, length = 100)
    private String deviceLabel;

    @Column(name = "fcm_push_token", length = 512)
    private String fcmPushToken;

    @Column(name = "public_identity_key", nullable = false, columnDefinition = "TEXT")
    private String publicIdentityKey;

    @Column(name = "registration_id", nullable = false)
    private int registrationId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserDevice() {}

    public UserDevice(User user, String deviceLabel, String publicIdentityKey, int registrationId) {
        this.user = user;
        this.deviceLabel = deviceLabel;
        this.publicIdentityKey = publicIdentityKey;
        this.registrationId = registrationId;
        this.createdAt = Instant.now();
        this.lastSeenAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }

    public String getFcmPushToken() { return fcmPushToken; }
    public void setFcmPushToken(String fcmPushToken) { this.fcmPushToken = fcmPushToken; }

    public String getPublicIdentityKey() { return publicIdentityKey; }
    public void setPublicIdentityKey(String publicIdentityKey) { this.publicIdentityKey = publicIdentityKey; }

    public int getRegistrationId() { return registrationId; }
    public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

