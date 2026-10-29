package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocked_users", uniqueConstraints = {
    @UniqueConstraint(name = "uq_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
})
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public BlockedUser() {}

    public BlockedUser(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getBlocker() { return blocker; }
    public void setBlocker(User blocker) { this.blocker = blocker; }

    public User getBlocked() { return blocked; }
    public void setBlocked(User blocked) { this.blocked = blocked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

