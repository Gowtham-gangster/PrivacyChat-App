package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "encrypted_attachments")
public class EncryptedAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    private String sha256Checksum;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType = "application/octet-stream";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EncryptedAttachment() {}

    public EncryptedAttachment(User uploader, String storagePath, long fileSizeBytes, String sha256Checksum, String mimeType) {
        this.uploader = uploader;
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Checksum = sha256Checksum;
        this.mimeType = mimeType;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }

    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

