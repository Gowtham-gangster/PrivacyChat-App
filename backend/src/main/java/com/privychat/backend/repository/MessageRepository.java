package com.privychat.backend.repository;

import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.Message;
import com.privychat.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.conversation = :conv AND m.createdAt < :beforeTime ORDER BY m.createdAt DESC")
    List<Message> findMessagesBeforeTimestamp(@Param("conv") Conversation conv, 
                                             @Param("beforeTime") Instant beforeTime, 
                                             Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId AND m.delivered = false ORDER BY m.createdAt ASC")
    List<Message> findPendingDeliveriesForRecipient(@Param("recipientId") UUID recipientId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLatestMessageInConversation(@Param("convId") UUID convId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId AND m.recipient.id = :userId AND m.read = false")
    long countUnreadMessages(@Param("convId") UUID convId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Message m SET m.delivered = true, m.deliveredAt = :now, m.status = 'DELIVERED' WHERE m.id IN :messageIds AND m.delivered = false")
    int markDelivered(@Param("messageIds") List<UUID> messageIds, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Message m SET m.read = true, m.readAt = :now, m.status = 'READ' WHERE m.id = :messageId AND m.read = false")
    int markRead(@Param("messageId") UUID messageId, @Param("now") Instant now);
}

