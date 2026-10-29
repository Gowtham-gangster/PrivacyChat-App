package com.privychat.backend.dto.chat;

import java.time.Instant;
import java.util.UUID;

public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private UUID recipientId;
    private String ciphertext;
    private String iv;
    private String ratchetPublicKey;
    private long sequenceNumber;
    private int previousCounter;
    private Integer oneTimePreKeyId;
    private String status;
    private boolean delivered;
    private boolean read;
    private Instant createdAt;

    public MessageResponse() {}

    public MessageResponse(UUID id, UUID conversationId, UUID senderId, UUID recipientId, String ciphertext, String iv, String ratchetPublicKey, long sequenceNumber, int previousCounter, Integer oneTimePreKeyId, String status, boolean delivered, boolean read, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.ratchetPublicKey = ratchetPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.previousCounter = previousCounter;
        this.oneTimePreKeyId = oneTimePreKeyId;
        this.status = status;
        this.delivered = delivered;
        this.read = read;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }

    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }

    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getRatchetPublicKey() { return ratchetPublicKey; }
    public void setRatchetPublicKey(String ratchetPublicKey) { this.ratchetPublicKey = ratchetPublicKey; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public int getPreviousCounter() { return previousCounter; }
    public void setPreviousCounter(int previousCounter) { this.previousCounter = previousCounter; }

    public Integer getOneTimePreKeyId() { return oneTimePreKeyId; }
    public void setOneTimePreKeyId(Integer oneTimePreKeyId) { this.oneTimePreKeyId = oneTimePreKeyId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

