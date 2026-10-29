package com.privychat.backend.dto.attachment;

import java.util.UUID;

public class AttachmentDownloadResponse {

    private UUID attachmentId;
    private String downloadUrl;
    private String signedUrl;
    private Integer expiresInSeconds;
    private long fileSizeBytes;
    private String sha256Checksum;
    private String mimeType;

    public AttachmentDownloadResponse() {}

    public AttachmentDownloadResponse(UUID attachmentId, String downloadUrl, long fileSizeBytes, String sha256Checksum, String mimeType) {
        this.attachmentId = attachmentId;
        this.downloadUrl = downloadUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Checksum = sha256Checksum;
        this.mimeType = mimeType;
    }

    public AttachmentDownloadResponse(UUID attachmentId, String downloadUrl, String signedUrl, Integer expiresInSeconds, long fileSizeBytes, String sha256Checksum, String mimeType) {
        this.attachmentId = attachmentId;
        this.downloadUrl = downloadUrl;
        this.signedUrl = signedUrl;
        this.expiresInSeconds = expiresInSeconds;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Checksum = sha256Checksum;
        this.mimeType = mimeType;
    }

    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getSignedUrl() { return signedUrl; }
    public void setSignedUrl(String signedUrl) { this.signedUrl = signedUrl; }

    public Integer getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Integer expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
