package com.privychat.backend.service;

import com.privychat.backend.dto.attachment.AttachmentDownloadResponse;
import com.privychat.backend.dto.attachment.AttachmentUploadRequest;
import com.privychat.backend.dto.attachment.AttachmentUploadResponse;
import com.privychat.backend.entity.EncryptedAttachment;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.EncryptedAttachmentRepository;
import com.privychat.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AttachmentStorageService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB
    private final EncryptedAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final String localDir;
    private final String appBaseUrl;

    public AttachmentStorageService(
            EncryptedAttachmentRepository attachmentRepository,
            UserRepository userRepository,
            @Value("${privychat.storage.local-dir:./storage-attachments}") String localDir,
            @Value("${privychat.app-base-url:http://localhost:8080}") String appBaseUrl) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.localDir = localDir;
        this.appBaseUrl = appBaseUrl;

        // Ensure storage directory exists
        try {
            Files.createDirectories(Paths.get(localDir));
        } catch (IOException e) {
            // Log or handle
        }
    }

    @Transactional
    public AttachmentUploadResponse requestUpload(UUID uploaderId, AttachmentUploadRequest req) {
        if (req.getFileSizeBytes() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 50MB limit");
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Uploader not found: " + uploaderId));

        UUID attachmentId = UUID.randomUUID();
        String storagePath = localDir + "/" + attachmentId + ".bin";

        EncryptedAttachment attachment = new EncryptedAttachment(
                uploader,
                storagePath,
                req.getFileSizeBytes(),
                req.getSha256Checksum(),
                req.getMimeType() != null ? req.getMimeType() : "application/octet-stream"
        );
        attachment.setId(attachmentId);
        attachmentRepository.save(attachment);

        String uploadUrl = appBaseUrl + "/api/v1/attachments/" + attachmentId + "/upload";
        return new AttachmentUploadResponse(attachmentId, uploadUrl, MAX_FILE_SIZE);
    }

    public void storeFile(UUID attachmentId, UUID userId, InputStream dataStream) throws IOException {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment record not found: " + attachmentId));

        if (!attachment.getUploader().getId().equals(userId)) {
            throw new UnauthorizedException("Forbidden: User does not own this attachment upload slot");
        }

        Path target = Paths.get(attachment.getStoragePath());
        Files.createDirectories(target.getParent());
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            dataStream.transferTo(fos);
        }
    }

    public AttachmentDownloadResponse getDownloadInfo(UUID attachmentId, UUID userId) {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        String downloadUrl = appBaseUrl + "/api/v1/attachments/" + attachmentId + "/file";
        return new AttachmentDownloadResponse(
                attachment.getId(),
                downloadUrl,
                attachment.getFileSizeBytes(),
                attachment.getSha256Checksum(),
                attachment.getMimeType()
        );
    }

    public Path getFilePath(UUID attachmentId) {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
        return Paths.get(attachment.getStoragePath());
    }
}

