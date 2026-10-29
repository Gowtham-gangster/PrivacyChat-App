package com.privychat.backend.repository;

import com.privychat.backend.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    long countByInviterIdAndCreatedAtAfter(UUID inviterId, java.time.Instant after);
}

