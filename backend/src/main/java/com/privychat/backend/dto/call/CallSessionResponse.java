package com.privychat.backend.dto.call;

import com.privychat.backend.dto.user.UserProfileResponse;

import java.time.Instant;
import java.util.UUID;

public class CallSessionResponse {

    private UUID id;
    private UserProfileResponse otherParty;
    private boolean isOutgoing;
    private String callType;
    private String status;
    private Instant startedAt;
    private Integer durationSeconds;

    public CallSessionResponse() {}

    public CallSessionResponse(UUID id, UserProfileResponse otherParty, boolean isOutgoing, String callType, String status, Instant startedAt, Integer durationSeconds) {
        this.id = id;
        this.otherParty = otherParty;
        this.isOutgoing = isOutgoing;
        this.callType = callType;
        this.status = status;
        this.startedAt = startedAt;
        this.durationSeconds = durationSeconds;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserProfileResponse getOtherParty() { return otherParty; }
    public void setOtherParty(UserProfileResponse otherParty) { this.otherParty = otherParty; }

    public boolean isOutgoing() { return isOutgoing; }
    public void setOutgoing(boolean outgoing) { isOutgoing = outgoing; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}

