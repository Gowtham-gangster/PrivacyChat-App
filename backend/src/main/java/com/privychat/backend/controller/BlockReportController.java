package com.privychat.backend.controller;

import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.BlockedUser;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.repository.BlockedUserRepository;
import com.privychat.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Blocking & Safety", description = "Block and unblock users")
public class BlockReportController {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    public BlockReportController(BlockedUserRepository blockedUserRepository, UserRepository userRepository) {
        this.blockedUserRepository = blockedUserRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/{targetUserId}/block")
    @Operation(summary = "Block a user from contacting you")
    @Transactional
    public ResponseEntity<Void> blockUser(@AuthenticationPrincipal UUID blockerId, @PathVariable UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocker not found"));
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (blockedUserRepository.findByBlockerAndBlocked(blocker, blocked).isEmpty()) {
            blockedUserRepository.save(new BlockedUser(blocker, blocked));
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetUserId}/block")
    @Operation(summary = "Unblock a user")
    @Transactional
    public ResponseEntity<Void> unblockUser(@AuthenticationPrincipal UUID blockerId, @PathVariable UUID targetUserId) {
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocker not found"));
        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        blockedUserRepository.findByBlockerAndBlocked(blocker, blocked).ifPresent(blockedUserRepository::delete);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked")
    @Operation(summary = "List all users blocked by the current user")
    public ResponseEntity<List<UserProfileResponse>> getBlockedUsers(@AuthenticationPrincipal UUID blockerId) {
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<BlockedUser> list = blockedUserRepository.findByBlocker(blocker);
        List<UserProfileResponse> responses = list.stream().map(b -> {
            User u = b.getBlocked();
            return new UserProfileResponse(
                    u.getId(),
                    u.getDisplayName(),
                    u.getEmail(),
                    u.getPhoneNumber(),
                    u.getAvatarUrl(),
                    u.isEmailVerified(),
                    u.isPhoneVerified()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}

