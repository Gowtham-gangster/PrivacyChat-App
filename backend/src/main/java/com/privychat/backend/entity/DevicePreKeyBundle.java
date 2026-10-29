package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_prekey_bundles", uniqueConstraints = {
    @UniqueConstraint(name = "uq_device_signed_prekey", columnNames = {"device_id", "signed_prekey_id"})
})
public class DevicePreKeyBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private UserDevice device;

    @Column(name = "signed_prekey_id", nullable = false)
    private int signedPreKeyId;

    @Column(name = "signed_prekey_public", nullable = false, columnDefinition = "TEXT")
    private String signedPreKeyPublic;

    @Column(name = "signed_prekey_signature", nullable = false, columnDefinition = "TEXT")
    private String signedPreKeySignature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DevicePreKeyBundle() {}

    public DevicePreKeyBundle(UserDevice device, int signedPreKeyId, String signedPreKeyPublic, String signedPreKeySignature) {
        this.device = device;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublic = signedPreKeyPublic;
        this.signedPreKeySignature = signedPreKeySignature;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserDevice getDevice() { return device; }
    public void setDevice(UserDevice device) { this.device = device; }

    public int getSignedPreKeyId() { return signedPreKeyId; }
    public void setSignedPreKeyId(int signedPreKeyId) { this.signedPreKeyId = signedPreKeyId; }

    public String getSignedPreKeyPublic() { return signedPreKeyPublic; }
    public void setSignedPreKeyPublic(String signedPreKeyPublic) { this.signedPreKeyPublic = signedPreKeyPublic; }

    public String getSignedPreKeySignature() { return signedPreKeySignature; }
    public void setSignedPreKeySignature(String signedPreKeySignature) { this.signedPreKeySignature = signedPreKeySignature; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

