package com.privychat.backend.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message implements Persistable<UUID> {

    @Id
    private UUID id; // Client-generated UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_device_id")
    private UserDevice senderDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_device_id")
    private UserDevice recipientDevice;

    @Column(name = "ciphertext", nullable = false, columnDefinition = "TEXT")
    private String ciphertext; // E2EE encrypted blob

    @Column(name = "iv", nullable = false, length = 64)
    private String iv;

    @Column(name = "ratchet_public_key", columnDefinition = "TEXT")
    private String ratchetPublicKey;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "previous_counter", nullable = false)
    private int previousCounter = 0;

    @Column(name = "one_time_prekey_id")
    private Integer oneTimePreKeyId;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACCEPTED"; // ACCEPTED, DELIVERED, READ, EXPIRED

    @Column(name = "is_delivered", nullable = false)
    private boolean delivered = false;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Transient
    private boolean isNew = false;

    public Message() {}

    public Message(UUID id, Conversation conversation, User sender, User recipient, 
                   String ciphertext, String iv, String ratchetPublicKey, long sequenceNumber) {
        this.id = id;
        this.conversation = conversation;
        this.sender = sender;
        this.recipient = recipient;
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.ratchetPublicKey = ratchetPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = Instant.now();
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public UserDevice getSenderDevice() { return senderDevice; }
    public void setSenderDevice(UserDevice senderDevice) { this.senderDevice = senderDevice; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public UserDevice getRecipientDevice() { return recipientDevice; }
    public void setRecipientDevice(UserDevice recipientDevice) { this.recipientDevice = recipientDevice; }

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

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

