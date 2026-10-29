package com.privychat.backend.service;

import com.privychat.backend.dto.attachment.AttachmentDownloadResponse;
import com.privychat.backend.dto.attachment.AttachmentUploadRequest;
import com.privychat.backend.dto.attachment.AttachmentUploadResponse;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.EncryptedAttachment;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.ConversationParticipantRepository;
import com.privychat.backend.repository.ConversationRepository;
import com.privychat.backend.repository.EncryptedAttachmentRepository;
import com.privychat.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AttachmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentStorageService.class);
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    private final EncryptedAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;
    private final SupabaseStorageService supabaseStorageService;

    private final String localDir;
    private final String appBaseUrl;

    public AttachmentStorageService(
            EncryptedAttachmentRepository attachmentRepository,
            UserRepository userRepository,
            ConversationParticipantRepository participantRepository,
            ConversationRepository conversationRepository,
            SupabaseStorageService supabaseStorageService,
            @Value("${privychat.storage.local-dir:./storage-attachments}") String localDir,
            @Value("${privychat.app-base-url:http://localhost:8080}") String appBaseUrl) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.conversationRepository = conversationRepository;
        this.supabaseStorageService = supabaseStorageService;
        this.localDir = localDir;
        this.appBaseUrl = appBaseUrl;

        // Ensure fallback local storage directory exists
        try {
            Files.createDirectories(Paths.get(localDir));
        } catch (IOException ignored) {}

        // Verify Supabase bucket if configured
        if (supabaseStorageService.isConfigured()) {
            supabaseStorageService.ensureBucketExists();
        }
    }

    @Transactional
    public AttachmentUploadResponse requestUpload(UUID uploaderId, AttachmentUploadRequest req) {
        if (req.getFileSizeBytes() <= 0 || req.getFileSizeBytes() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must be between 1 byte and 50MB");
        }

        if (req.getSha256Checksum() == null || !SHA256_PATTERN.matcher(req.getSha256Checksum()).matches()) {
            throw new BadRequestException("Invalid SHA-256 checksum format: must be 64 hexadecimal characters");
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Uploader not found: " + uploaderId));

        Conversation conversation = null;
        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + req.getConversationId()));

            boolean isParticipant = participantRepository.findByConversationIdAndUserId(req.getConversationId(), uploaderId).isPresent();
            if (!isParticipant) {
                throw new UnauthorizedException("Forbidden: User is not an authorized participant in conversation " + req.getConversationId());
            }
        }

        UUID attachmentId = UUID.randomUUID();
        String safeMimeType = sanitizeMimeType(req.getMimeType());

        String relativePath = "conversations/" + (conversation != null ? conversation.getId() : "direct") + "/" + attachmentId + ".bin";
        String storagePath = supabaseStorageService.isConfigured()
                ? "supabase://" + supabaseStorageService.getBucketName() + "/" + relativePath
                : localDir + "/" + attachmentId + ".bin";

        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                uploader,
                storagePath,
                req.getFileSizeBytes(),
                req.getSha256Checksum().toLowerCase(),
                safeMimeType
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

        // Read stream into byte buffer and verify integrity
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        long totalBytes = 0;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest unavailable", e);
        }

        while ((bytesRead = dataStream.read(chunk)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > MAX_FILE_SIZE) {
                throw new BadRequestException("Uploaded ciphertext exceeds maximum permitted limit of 50MB");
            }
            digest.update(chunk, 0, bytesRead);
            buffer.write(chunk, 0, bytesRead);
        }

        byte[] ciphertext = buffer.toByteArray();
        String actualChecksum = HexFormat.of().formatHex(digest.digest());

        if (!actualChecksum.equalsIgnoreCase(attachment.getSha256Checksum())) {
            throw new BadRequestException(String.format("Corrupted ciphertext: SHA-256 checksum mismatch (expected %s, got %s)",
                    attachment.getSha256Checksum(), actualChecksum));
        }

        // Store to Supabase Storage if configured; otherwise local fallback
        if (supabaseStorageService.isConfigured() && attachment.getStoragePath().startsWith("supabase://")) {
            String objectPath = extractObjectPath(attachment.getStoragePath());
            try {
                supabaseStorageService.uploadCiphertext(objectPath, ciphertext, attachment.getMimeType());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Supabase Storage upload interrupted", e);
            }
        } else {
            Path target = Paths.get(attachment.getStoragePath());
            Files.createDirectories(target.getParent());
            try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
                fos.write(ciphertext);
            }
        }
    }

    public AttachmentDownloadResponse getDownloadInfo(UUID attachmentId, UUID userId) {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        authorizeAccess(attachment, userId);

        String directUrl = appBaseUrl + "/api/v1/attachments/" + attachmentId + "/file";
        String signedUrl = null;
        Integer expiresInSeconds = null;

        if (supabaseStorageService.isConfigured() && attachment.getStoragePath().startsWith("supabase://")) {
            String objectPath = extractObjectPath(attachment.getStoragePath());
            signedUrl = supabaseStorageService.createSignedDownloadUrl(objectPath, supabaseStorageService.getSignedUrlExpiration());
            expiresInSeconds = supabaseStorageService.getSignedUrlExpiration();
        }

        return new AttachmentDownloadResponse(
                attachment.getId(),
                directUrl,
                signedUrl,
                expiresInSeconds,
                attachment.getFileSizeBytes(),
                attachment.getSha256Checksum(),
                attachment.getMimeType()
        );
    }

    public byte[] downloadBytes(UUID attachmentId, UUID userId) throws IOException {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        authorizeAccess(attachment, userId);

        if (supabaseStorageService.isConfigured() && attachment.getStoragePath().startsWith("supabase://")) {
            String objectPath = extractObjectPath(attachment.getStoragePath());
            try {
                return supabaseStorageService.downloadCiphertext(objectPath);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Supabase Storage download interrupted", e);
            }
        } else {
            Path filePath = Paths.get(attachment.getStoragePath());
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("Attachment file not found on local disk");
            }
            return Files.readAllBytes(filePath);
        }
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID userId) {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        authorizeAccess(attachment, userId);

        // Delete from Supabase Storage
        if (supabaseStorageService.isConfigured() && attachment.getStoragePath().startsWith("supabase://")) {
            String objectPath = extractObjectPath(attachment.getStoragePath());
            supabaseStorageService.deleteObject(objectPath);
        } else {
            try {
                Files.deleteIfExists(Paths.get(attachment.getStoragePath()));
            } catch (IOException ignored) {}
        }

        attachmentRepository.delete(attachment);
        log.info("Encrypted attachment {} successfully deleted by user {}", attachmentId, userId);
    }

    public Path getFilePath(UUID attachmentId) {
        EncryptedAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
        return Paths.get(attachment.getStoragePath());
    }

    private void authorizeAccess(EncryptedAttachment attachment, UUID userId) {
        if (attachment.getUploader().getId().equals(userId)) {
            return; // Uploader is always authorized
        }

        if (attachment.getConversation() != null) {
            boolean isParticipant = participantRepository.findByConversationIdAndUserId(
                    attachment.getConversation().getId(), userId).isPresent();
            if (isParticipant) {
                return; // Conversation peer is authorized
            }
        }

        throw new UnauthorizedException("Forbidden: Caller is not an authorized participant for this attachment");
    }

    private String extractObjectPath(String storagePath) {
        if (storagePath.startsWith("supabase://")) {
            // format: supabase://bucketName/conversations/...
            int secondSlash = storagePath.indexOf('/', "supabase://".length());
            if (secondSlash != -1) {
                return storagePath.substring(secondSlash + 1);
            }
        }
        return storagePath;
    }

    private String sanitizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        String clean = mimeType.trim().toLowerCase();
        if (clean.length() > 128) {
            clean = clean.substring(0, 128);
        }
        return clean;
    }
}
