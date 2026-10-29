package com.privychat.backend.service;

import com.privychat.backend.dto.attachment.AttachmentDownloadResponse;
import com.privychat.backend.dto.attachment.AttachmentUploadRequest;
import com.privychat.backend.dto.attachment.AttachmentUploadResponse;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.ConversationParticipant;
import com.privychat.backend.entity.EncryptedAttachment;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.ConversationParticipantRepository;
import com.privychat.backend.repository.ConversationRepository;
import com.privychat.backend.repository.EncryptedAttachmentRepository;
import com.privychat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentStorageServiceTest {

    @Mock
    private EncryptedAttachmentRepository attachmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private AttachmentStorageService storageService;

    private User alice;
    private User bob;
    private User eve;
    private Conversation conversation;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        storageService = new AttachmentStorageService(
                attachmentRepository,
                userRepository,
                participantRepository,
                conversationRepository,
                supabaseStorageService,
                "./target/test-storage",
                "http://localhost:8080"
        );

        alice = new User("alice@example.com", "+12025550101", "hash", "Alice");
        alice.setId(UUID.randomUUID());

        bob = new User("bob@example.com", "+12025550102", "hash", "Bob");
        bob.setId(UUID.randomUUID());

        eve = new User("eve@example.com", "+12025550103", "hash", "Eve");
        eve.setId(UUID.randomUUID());

        conversationId = UUID.randomUUID();
        conversation = new Conversation(alice, bob);
        conversation.setId(conversationId);
    }

    @Test
    @DisplayName("Authorized participant can successfully request upload slot")
    void testRequestUploadSuccessWithValidParticipant() {
        when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdAndUserId(conversationId, alice.getId()))
                .thenReturn(Optional.of(new ConversationParticipant(conversation, alice)));

        String validSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        AttachmentUploadRequest req = new AttachmentUploadRequest(
                UUID.randomUUID(),
                conversationId,
                1024L,
                validSha256,
                "application/octet-stream"
        );

        AttachmentUploadResponse resp = storageService.requestUpload(alice.getId(), req);

        assertNotNull(resp);
        assertNotNull(resp.getAttachmentId());
        assertTrue(resp.getUploadUrl().contains(resp.getAttachmentId().toString()));
        assertEquals(50L * 1024 * 1024, resp.getMaxSizeBytes());
        verify(attachmentRepository, times(1)).save(any(EncryptedAttachment.class));
    }

    @Test
    @DisplayName("Non-participant requesting upload is rejected with 403 Forbidden")
    void testRequestUploadRejectedForNonParticipant() {
        when(userRepository.findById(eve.getId())).thenReturn(Optional.of(eve));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdAndUserId(conversationId, eve.getId()))
                .thenReturn(Optional.empty());

        AttachmentUploadRequest req = new AttachmentUploadRequest(
                UUID.randomUUID(),
                conversationId,
                1024L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "image/png"
        );

        assertThrows(UnauthorizedException.class, () -> storageService.requestUpload(eve.getId(), req));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Request with file exceeding 50MB is rejected with 400 Bad Request")
    void testRequestUploadRejectedForFileExceeding50MB() {
        AttachmentUploadRequest req = new AttachmentUploadRequest(
                UUID.randomUUID(),
                conversationId,
                51L * 1024 * 1024, // 51 MB
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "video/mp4"
        );

        assertThrows(BadRequestException.class, () -> storageService.requestUpload(alice.getId(), req));
    }

    @Test
    @DisplayName("Request with malformed SHA-256 checksum format is rejected")
    void testRequestUploadRejectedForInvalidChecksumFormat() {
        AttachmentUploadRequest req = new AttachmentUploadRequest(
                UUID.randomUUID(),
                conversationId,
                1024L,
                "invalid-sha256-string",
                "image/jpeg"
        );

        assertThrows(BadRequestException.class, () -> storageService.requestUpload(alice.getId(), req));
    }

    @Test
    @DisplayName("Uploading ciphertext with matching SHA-256 checksum succeeds")
    void testStoreFileSuccessWithValidCiphertextAndChecksum() throws Exception {
        byte[] ciphertext = "Encrypted Ciphertext AES-256-GCM Payload".getBytes();
        String sha256 = computeSha256(ciphertext);

        UUID attachmentId = UUID.randomUUID();
        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                "./target/test-storage/" + attachmentId + ".bin",
                ciphertext.length,
                sha256,
                "application/octet-stream"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        storageService.storeFile(attachmentId, alice.getId(), new ByteArrayInputStream(ciphertext));
    }

    @Test
    @DisplayName("Uploading corrupted ciphertext with mismatched checksum fails with 400")
    void testStoreFileFailsWhenCiphertextIsCorrupted() {
        byte[] original = "Original Valid Ciphertext".getBytes();
        String originalChecksum = computeSha256(original);

        byte[] corrupted = "Tampered Corrupted Ciphertext".getBytes();

        UUID attachmentId = UUID.randomUUID();
        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                "./target/test-storage/" + attachmentId + ".bin",
                corrupted.length,
                originalChecksum,
                "application/octet-stream"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(BadRequestException.class, () ->
                storageService.storeFile(attachmentId, alice.getId(), new ByteArrayInputStream(corrupted)));
    }

    @Test
    @DisplayName("User cannot upload data to an attachment slot created by another user")
    void testStoreFileUnauthorizedForDifferentUser() {
        UUID attachmentId = UUID.randomUUID();
        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                "./target/test-storage/" + attachmentId + ".bin",
                100L,
                "sha256",
                "application/octet-stream"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThrows(UnauthorizedException.class, () ->
                storageService.storeFile(attachmentId, eve.getId(), new ByteArrayInputStream("data".getBytes())));
    }

    @Test
    @DisplayName("Conversation peer can get download info with Supabase signed URL")
    void testGetDownloadInfoAuthorizedForConversationParticipant() {
        UUID attachmentId = UUID.randomUUID();
        String storagePath = "supabase://privychat-attachments/conversations/" + conversationId + "/" + attachmentId + ".bin";

        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                storagePath,
                2048L,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "application/pdf"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(participantRepository.findByConversationIdAndUserId(conversationId, bob.getId()))
                .thenReturn(Optional.of(new ConversationParticipant(conversation, bob)));
        when(supabaseStorageService.isConfigured()).thenReturn(true);
        when(supabaseStorageService.getSignedUrlExpiration()).thenReturn(300);
        when(supabaseStorageService.createSignedDownloadUrl(anyString(), eq(300)))
                .thenReturn("https://project.supabase.co/storage/v1/object/sign/privychat-attachments/...?token=xyz");

        AttachmentDownloadResponse resp = storageService.getDownloadInfo(attachmentId, bob.getId());

        assertNotNull(resp);
        assertEquals(attachmentId, resp.getAttachmentId());
        assertNotNull(resp.getSignedUrl());
        assertEquals(300, resp.getExpiresInSeconds());
        assertTrue(resp.getSignedUrl().contains("token=xyz"));
    }

    @Test
    @DisplayName("Unauthorized user cannot retrieve attachment download info")
    void testGetDownloadInfoForbiddenForNonParticipant() {
        UUID attachmentId = UUID.randomUUID();
        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                "path",
                2048L,
                "sha256",
                "application/pdf"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(participantRepository.findByConversationIdAndUserId(conversationId, eve.getId()))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> storageService.getDownloadInfo(attachmentId, eve.getId()));
    }

    @Test
    @DisplayName("Participant can delete attachment from storage and database")
    void testDeleteAttachmentAuthorizedForParticipant() {
        UUID attachmentId = UUID.randomUUID();
        String storagePath = "supabase://privychat-attachments/conversations/" + conversationId + "/" + attachmentId + ".bin";

        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                storagePath,
                1024L,
                "sha256",
                "audio/ogg"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(supabaseStorageService.isConfigured()).thenReturn(true);

        storageService.deleteAttachment(attachmentId, alice.getId());

        verify(supabaseStorageService, times(1)).deleteObject(anyString());
        verify(attachmentRepository, times(1)).delete(attachment);
    }

    @Test
    @DisplayName("Non-participant cannot delete an attachment")
    void testDeleteAttachmentForbiddenForNonParticipant() {
        UUID attachmentId = UUID.randomUUID();
        EncryptedAttachment attachment = new EncryptedAttachment(
                conversation,
                alice,
                "path",
                1024L,
                "sha256",
                "audio/ogg"
        );
        attachment.setId(attachmentId);

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(participantRepository.findByConversationIdAndUserId(conversationId, eve.getId()))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> storageService.deleteAttachment(attachmentId, eve.getId()));
        verify(attachmentRepository, never()).delete(any());
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

