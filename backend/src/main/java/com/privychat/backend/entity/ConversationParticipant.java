package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
    @UniqueConstraint(name = "uq_conversation_member", columnNames = {"conversation_id", "user_id"})
})
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    public ConversationParticipant() {}

    public ConversationParticipant(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public UUID getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(UUID lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }
}

