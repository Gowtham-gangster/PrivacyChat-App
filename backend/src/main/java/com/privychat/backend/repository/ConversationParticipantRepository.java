package com.privychat.backend.repository;

import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.ConversationParticipant;
import com.privychat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);
    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    List<ConversationParticipant> findByConversation(Conversation conversation);
    List<ConversationParticipant> findByUserId(UUID userId);
}

