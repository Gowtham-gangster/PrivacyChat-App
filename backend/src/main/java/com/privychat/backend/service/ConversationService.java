package com.privychat.backend.service;

import com.privychat.backend.dto.chat.ConversationResponse;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.ConversationParticipant;
import com.privychat.backend.entity.Message;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final BlockedUserRepository blockedUserRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationParticipantRepository participantRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               BlockedUserRepository blockedUserRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.blockedUserRepository = blockedUserRepository;
    }

    public List<ConversationResponse> getUserConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);
        List<ConversationResponse> responses = new ArrayList<>();

        for (Conversation conv : conversations) {
            User otherUser = conv.getOtherParticipant(userId);
            if (otherUser == null) continue;

            Optional<Message> latestMsg = messageRepository.findLatestMessageInConversation(conv.getId());
            Instant lastTimestamp = latestMsg.map(Message::getCreatedAt).orElse(conv.getCreatedAt());
            long unreadCount = messageRepository.countUnreadMessages(conv.getId(), userId);

            responses.add(new ConversationResponse(
                    conv.getId(),
                    mapToProfile(otherUser),
                    lastTimestamp,
                    unreadCount,
                    conv.getCreatedAt()
            ));
        }

        return responses;
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(UUID userId, UUID recipientUserId) {
        if (userId.equals(recipientUserId)) {
            throw new BadRequestException("Cannot establish a conversation with yourself");
        }

        if (blockedUserRepository.isBlockedEitherWay(userId, recipientUserId)) {
            throw new BadRequestException("Cannot establish conversation with a blocked user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found: " + recipientUserId));

        Conversation conversation = conversationRepository.findBetweenUsers(user, recipient)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation(user, recipient);
                    newConv = conversationRepository.save(newConv);

                    ConversationParticipant p1 = new ConversationParticipant(newConv, user);
                    ConversationParticipant p2 = new ConversationParticipant(newConv, recipient);
                    participantRepository.save(p1);
                    participantRepository.save(p2);

                    return newConv;
                });

        long unreadCount = messageRepository.countUnreadMessages(conversation.getId(), userId);
        Optional<Message> latestMsg = messageRepository.findLatestMessageInConversation(conversation.getId());

        return new ConversationResponse(
                conversation.getId(),
                mapToProfile(recipient),
                latestMsg.map(Message::getCreatedAt).orElse(conversation.getCreatedAt()),
                unreadCount,
                conversation.getCreatedAt()
        );
    }

    public Conversation getAuthorizedConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedException("Forbidden: User is not an authorized participant in conversation " + conversationId);
        }

        return conversation;
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

