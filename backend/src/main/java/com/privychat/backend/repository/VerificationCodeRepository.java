package com.privychat.backend.repository;

import com.privychat.backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    Optional<VerificationCode> findTopByIdentifierAndTypeOrderByCreatedAtDesc(String identifier, String type);
    void deleteByIdentifierAndType(String identifier, String type);
}

