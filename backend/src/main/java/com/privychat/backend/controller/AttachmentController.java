package com.privychat.backend.controller;

import com.privychat.backend.dto.attachment.AttachmentDownloadResponse;
import com.privychat.backend.dto.attachment.AttachmentUploadRequest;
import com.privychat.backend.dto.attachment.AttachmentUploadResponse;
import com.privychat.backend.service.AttachmentStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attachments")
@Tag(name = "Attachments", description = "Encrypted media and document storage authorization, streaming, and deletion")
public class AttachmentController {

    private final AttachmentStorageService storageService;

    public AttachmentController(AttachmentStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload-request")
    @Operation(summary = "Request authorization to upload an encrypted attachment")
    public ResponseEntity<AttachmentUploadResponse> requestUpload(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AttachmentUploadRequest req) {
        return ResponseEntity.ok(storageService.requestUpload(userId, req));
    }

    @PutMapping(value = "/{attachmentId}/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Stream raw encrypted ciphertext bytes for an authorized attachment")
    public ResponseEntity<Void> uploadFile(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest request) throws IOException {
        storageService.storeFile(attachmentId, userId, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Get download URL and checksum verification details for an attachment")
    public ResponseEntity<AttachmentDownloadResponse> getDownloadInfo(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(storageService.getDownloadInfo(attachmentId, userId));
    }

    @GetMapping("/{attachmentId}/file")
    @Operation(summary = "Stream encrypted attachment bytes (proxied securely from storage)")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UUID userId) throws IOException {
        byte[] bytes = storageService.downloadBytes(attachmentId, userId);
        Resource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentId + ".bin\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete an attachment from storage and database (authorized participants only)")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UUID userId) {
        storageService.deleteAttachment(attachmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
