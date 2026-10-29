package com.privychat.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callee_id", nullable = false)
    private User callee;

    @Column(name = "call_type", nullable = false, length = 16)
    private String callType; // AUDIO, VIDEO

    @Column(name = "status", nullable = false, length = 32)
    private String status = "INITIATED"; // INITIATED, RINGING, CONNECTED, REJECTED, MISSED, ENDED

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    public CallSession() {}

    public CallSession(User caller, User callee, String callType) {
        this.caller = caller;
        this.callee = callee;
        this.callType = callType;
        this.status = "INITIATED";
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getCaller() { return caller; }
    public void setCaller(User caller) { this.caller = caller; }

    public User getCallee() { return callee; }
    public void setCallee(User callee) { this.callee = callee; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}

