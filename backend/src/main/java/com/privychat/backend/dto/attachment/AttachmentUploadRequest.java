package com.privychat.backend.dto.attachment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class AttachmentUploadRequest {

    private UUID messageId;

    @Min(value = 1, message = "File size must be greater than 0")
    private long fileSizeBytes;

    @NotBlank(message = "SHA-256 checksum is required")
    private String sha256Checksum;

    private String mimeType = "application/octet-stream";

    public AttachmentUploadRequest() {}

    public AttachmentUploadRequest(UUID messageId, long fileSizeBytes, String sha256Checksum, String mimeType) {
        this.messageId = messageId;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Checksum = sha256Checksum;
        this.mimeType = mimeType;
    }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}

