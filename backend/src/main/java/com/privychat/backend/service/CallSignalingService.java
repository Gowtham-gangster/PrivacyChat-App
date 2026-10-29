package com.privychat.backend.service;

import com.privychat.backend.dto.call.CallSessionResponse;
import com.privychat.backend.dto.call.CallSignalMessage;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.CallSession;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.repository.BlockedUserRepository;
import com.privychat.backend.repository.CallSessionRepository;
import com.privychat.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CallSignalingService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CallSignalingService(CallSessionRepository callSessionRepository,
                                UserRepository userRepository,
                                BlockedUserRepository blockedUserRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.callSessionRepository = callSessionRepository;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void handleSignal(UUID senderId, CallSignalMessage signal) {
        if (blockedUserRepository.isBlockedEitherWay(senderId, signal.getRecipientId())) {
            throw new BadRequestException("Cannot establish call with a blocked user");
        }

        signal.setCallerId(senderId);

        if ("OFFER".equalsIgnoreCase(signal.getSignalType())) {
            User caller = userRepository.findById(senderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));
            User callee = userRepository.findById(signal.getRecipientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Callee not found"));

            CallSession session = new CallSession(caller, callee, signal.getCallType());
            if (signal.getCallId() != null) {
                session.setId(signal.getCallId());
            }
            session = callSessionRepository.save(session);
            signal.setCallId(session.getId());
        } else if ("ANSWER".equalsIgnoreCase(signal.getSignalType())) {
            if (signal.getCallId() != null) {
                callSessionRepository.findById(signal.getCallId()).ifPresent(s -> {
                    s.setStatus("CONNECTED");
                    callSessionRepository.save(s);
                });
            }
        } else if ("DECLINE".equalsIgnoreCase(signal.getSignalType()) || "HANGUP".equalsIgnoreCase(signal.getSignalType())) {
            if (signal.getCallId() != null) {
                callSessionRepository.findById(signal.getCallId()).ifPresent(s -> {
                    s.setStatus("DECLINE".equalsIgnoreCase(signal.getSignalType()) ? "REJECTED" : "ENDED");
                    s.setEndedAt(Instant.now());
                    if (s.getStartedAt() != null) {
                        s.setDurationSeconds((int) Duration.between(s.getStartedAt(), s.getEndedAt()).getSeconds());
                    }
                    callSessionRepository.save(s);
                });
            }
        }

        // Deliver signal to recipient
        messagingTemplate.convertAndSend("/topic/user." + signal.getRecipientId() + ".call", signal);
    }

    public List<CallSessionResponse> getCallHistory(UUID userId, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 50);
        List<CallSession> sessions = callSessionRepository.findUserCallHistory(userId, PageRequest.of(0, pageSize));
        List<CallSessionResponse> responses = new ArrayList<>();

        for (CallSession s : sessions) {
            boolean isOutgoing = s.getCaller().getId().equals(userId);
            User otherUser = isOutgoing ? s.getCallee() : s.getCaller();
            responses.add(new CallSessionResponse(
                    s.getId(),
                    mapToProfile(otherUser),
                    isOutgoing,
                    s.getCallType(),
                    s.getStatus(),
                    s.getStartedAt(),
                    s.getDurationSeconds()
            ));
        }

        return responses;
    }

    private UserProfileResponse mapToProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.isPhoneVerified()
        );
    }
}

