package com.privychat.backend.repository;

import com.privychat.backend.entity.EncryptedAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncryptedAttachmentRepository extends JpaRepository<EncryptedAttachment, UUID> {
    Optional<EncryptedAttachment> findByIdAndUploaderId(UUID id, UUID uploaderId);
}

