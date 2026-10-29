package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_canonical_participants", columnNames = {"participant1_id", "participant2_id"})
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Conversation() {}

    public Conversation(User participant1, User participant2) {
        // Enforce canonical ordering of participants by UUID string representation
        if (participant1.getId().compareTo(participant2.getId()) < 0) {
            this.participant1 = participant1;
            this.participant2 = participant2;
        } else {
            this.participant1 = participant2;
            this.participant2 = participant1;
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean hasParticipant(UUID userId) {
        return participant1.getId().equals(userId) || participant2.getId().equals(userId);
    }

    public User getOtherParticipant(UUID userId) {
        if (participant1.getId().equals(userId)) return participant2;
        if (participant2.getId().equals(userId)) return participant1;
        return null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getParticipant1() { return participant1; }
    public void setParticipant1(User participant1) { this.participant1 = participant1; }

    public User getParticipant2() { return participant2; }
    public void setParticipant2(User participant2) { this.participant2 = participant2; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

