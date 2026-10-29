package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "one_time_prekeys", uniqueConstraints = {
    @UniqueConstraint(name = "uq_device_otpk", columnNames = {"device_id", "key_id"})
})
public class OneTimePreKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private UserDevice device;

    @Column(name = "key_id", nullable = false)
    private int keyId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OneTimePreKey() {}

    public OneTimePreKey(UserDevice device, int keyId, String publicKey) {
        this.device = device;
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.consumed = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserDevice getDevice() { return device; }
    public void setDevice(UserDevice device) { this.device = device; }

    public int getKeyId() { return keyId; }
    public void setKeyId(int keyId) { this.keyId = keyId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

