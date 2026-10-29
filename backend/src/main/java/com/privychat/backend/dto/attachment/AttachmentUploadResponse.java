package com.privychat.backend.dto.attachment;

import java.util.UUID;

public class AttachmentUploadResponse {

    private UUID attachmentId;
    private String uploadUrl;
    private long maxSizeBytes;

    public AttachmentUploadResponse() {}

    public AttachmentUploadResponse(UUID attachmentId, String uploadUrl, long maxSizeBytes) {
        this.attachmentId = attachmentId;
        this.uploadUrl = uploadUrl;
        this.maxSizeBytes = maxSizeBytes;
    }

    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }

    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }

    public long getMaxSizeBytes() { return maxSizeBytes; }
    public void setMaxSizeBytes(long maxSizeBytes) { this.maxSizeBytes = maxSizeBytes; }
}

