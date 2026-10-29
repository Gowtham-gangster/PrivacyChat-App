package com.privychat.backend.service;

import com.privychat.backend.dto.invite.InvitationResponse;
import com.privychat.backend.dto.invite.SendInvitationRequest;
import com.privychat.backend.entity.Invitation;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.RateLimitExceededException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.repository.InvitationRepository;
import com.privychat.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             EmailService emailService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public InvitationResponse sendInvitation(UUID inviterId, SendInvitationRequest req) {
        // Enforce rate limiting: max 10 invitations per hour
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (invitationRepository.countByInviterIdAndCreatedAtAfter(inviterId, oneHourAgo) >= 10) {
            throw new RateLimitExceededException("Hourly invitation limit reached (maximum 10 per hour)");
        }

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("Inviter user not found: " + inviterId));

        // Generate 256-bit random opaque token
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = HexFormat.of().formatHex(tokenBytes);

        // Compute SHA-256 hash for database persistence
        String tokenHash = hashToken(rawToken);

        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);
        Invitation invitation = new Invitation(inviter, req.getRecipientEmail().toLowerCase().trim(), tokenHash, expiry);
        invitation = invitationRepository.save(invitation);

        // Send HTML invitation email
        emailService.sendFriendInvitationEmail(invitation.getRecipientEmail(), inviter.getDisplayName(), rawToken);

        return new InvitationResponse(
                invitation.getId(),
                invitation.getRecipientEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing in JVM", e);
        }
    }
}

