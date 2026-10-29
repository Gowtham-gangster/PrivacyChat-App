package com.privychat.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SendMessageRequest {

    @NotNull(message = "Message ID is required")
    private UUID messageId;

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    private UUID recipientDeviceId;

    @NotBlank(message = "Ciphertext is required")
    private String ciphertext;

    @NotBlank(message = "IV is required")
    private String iv;

    private String ratchetPublicKey;

    private long sequenceNumber;

    private int previousCounter;

    private Integer oneTimePreKeyId;

    public SendMessageRequest() {}

    public SendMessageRequest(UUID messageId, UUID conversationId, UUID recipientId, String ciphertext, String iv, String ratchetPublicKey, long sequenceNumber) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.recipientId = recipientId;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.ratchetPublicKey = ratchetPublicKey;
        this.sequenceNumber = sequenceNumber;
    }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public UUID getRecipientId() { return recipientId; }
    public void setRecipientId(UUID recipientId) { this.recipientId = recipientId; }

    public UUID getRecipientDeviceId() { return recipientDeviceId; }
    public void setRecipientDeviceId(UUID recipientDeviceId) { this.recipientDeviceId = recipientDeviceId; }

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
}

