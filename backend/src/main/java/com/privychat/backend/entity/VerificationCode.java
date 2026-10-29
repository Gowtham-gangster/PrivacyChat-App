package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_codes")
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "identifier", nullable = false, length = 255)
    private String identifier; // Email or phone number

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "type", nullable = false, length = 32)
    private String type; // EMAIL_VERIFICATION, PHONE_VERIFICATION, PASSWORD_RESET

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VerificationCode() {}

    public VerificationCode(String identifier, String codeHash, String type, Instant expiresAt) {
        this.identifier = identifier;
        this.codeHash = codeHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

