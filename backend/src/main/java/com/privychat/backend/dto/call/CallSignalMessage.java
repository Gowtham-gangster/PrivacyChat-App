package com.privychat.backend.dto.call;

import java.util.UUID;

public class CallSignalMessage {

    private UUID callId;
    private UUID callerId;
    private UUID recipientId;
    private String signalType; // OFFER, ANSWER, ICE_CANDIDATE, RINGING, DECLINE, HANGUP
    private String callType;   // AUDIO, VIDEO
    private String sdp;
    private IceCandidateDto candidate;

    public static class IceCandidateDto {
        private String sdpMid;
        private int sdpMLineIndex;
        private String candidate;

        public IceCandidateDto() {}
        public IceCandidateDto(String sdpMid, int sdpMLineIndex, String candidate) {
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
            this.candidate = candidate;
        }

        public String getSdpMid() { return sdpMid; }
        public void setSdpMid(String sdpMid) { this.sdpMid = sdpMid; }
        public int getSdpMLineIndex() { return sdpMLineIndex; }
        public void setSdpMLineIndex(int sdpMLineIndex) { this.sdpMLineIndex = sdpMLineIndex; }
        public String getCandidate() { return candidate; }
        public void setCandidate(String candidate) { this.candidate = candidate; }
    }

    public CallSignalMessage() {}

    public CallSignalMessage(UUID callId, UUID callerId, UUID recipientId, String signalType, String callType) {
        this.callId = callId;
        this.callerId = callerId;
        this.recipientId = recipientId;
        this.signalType = signalType;
        this.callType = callType;
    }

    public UUID getCallId() { return callId; }
    public void setCallId(UUID callId) { this.callId = callId; }

    public UUID getCallerId() { return callerId; }
    public void setCallerId(UUID callerId) { this.callerId = callerId; }

    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public IceCandidateDto getCandidate() { return candidate; }
    public void setCandidate(IceCandidateDto candidate) { this.candidate = candidate; }
}

