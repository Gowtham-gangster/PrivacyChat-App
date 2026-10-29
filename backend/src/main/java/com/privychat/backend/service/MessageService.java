package com.privychat.backend.service;

import com.privychat.backend.dto.chat.MessageReceiptDto;
import com.privychat.backend.dto.chat.MessageResponse;
import com.privychat.backend.dto.chat.SendMessageRequest;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.Message;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserDevice;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.BlockedUserRepository;
import com.privychat.backend.repository.MessageRepository;
import com.privychat.backend.repository.UserDeviceRepository;
import com.privychat.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationService conversationService,
                          UserRepository userRepository,
                          UserDeviceRepository userDeviceRepository,
                          BlockedUserRepository blockedUserRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest req) {
        // Verify conversation membership
        Conversation conversation = conversationService.getAuthorizedConversation(req.getConversationId(), senderId);

        if (blockedUserRepository.isBlockedEitherWay(senderId, req.getRecipientId())) {
            throw new BadRequestException("Cannot send messages to a blocked user");
        }

        // Deduplication check: if messageId already exists, return existing
        Optional<Message> existing = messageRepository.findById(req.getMessageId());
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        User sender = userRepository.getReferenceById(senderId);
        User recipient = userRepository.findById(req.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found: " + req.getRecipientId()));

        UserDevice recipientDevice = req.getRecipientDeviceId() != null
                ? userDeviceRepository.findById(req.getRecipientDeviceId()).orElse(null)
                : null;

        Message message = new Message(
                req.getMessageId(),
                conversation,
                sender,
                recipient,
                req.getCiphertext(),
                req.getIv(),
                req.getRatchetPublicKey(),
                req.getSequenceNumber()
        );
        message.setPreviousCounter(req.getPreviousCounter());
        message.setOneTimePreKeyId(req.getOneTimePreKeyId());
        message.setRecipientDevice(recipientDevice);
        message.setStatus("ACCEPTED");

        message = messageRepository.save(message);

        conversation.setUpdatedAt(Instant.now());

        MessageResponse response = mapToResponse(message);

        // Real-time delivery to recipient topic
        messagingTemplate.convertAndSend("/topic/user." + recipient.getId() + ".messages", response);

        // Acknowledge sender with SENT receipt
        Map<String, Object> ack = new HashMap<>();
        ack.put("messageId", message.getId());
        ack.put("conversationId", conversation.getId());
        ack.put("status", "SENT");
        ack.put("timestamp", message.getCreatedAt().toString());
        messagingTemplate.convertAndSend("/topic/user." + senderId + ".receipts", ack);

        return response;
    }

    public List<MessageResponse> getConversationMessages(UUID conversationId, UUID userId, Instant before, int limit) {
        Conversation conv = conversationService.getAuthorizedConversation(conversationId, userId);
        Instant beforeTime = before != null ? before : Instant.now();
        int pageSize = Math.min(Math.max(limit, 1), 100);

        List<Message> messages = messageRepository.findMessagesBeforeTimestamp(conv, beforeTime, PageRequest.of(0, pageSize));
        return messages.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void markReceipts(UUID userId, MessageReceiptDto dto) {
        Instant now = Instant.now();
        if ("DELIVERED".equalsIgnoreCase(dto.getType())) {
            messageRepository.markDelivered(dto.getMessageIds(), now);
        } else if ("READ".equalsIgnoreCase(dto.getType())) {
            for (UUID msgId : dto.getMessageIds()) {
                messageRepository.markRead(msgId, now);
            }
        }

        // Notify senders of receipt update
        for (UUID msgId : dto.getMessageIds()) {
            messageRepository.findById(msgId).ifPresent(msg -> {
                Map<String, Object> receiptNotice = new HashMap<>();
                receiptNotice.put("messageId", msg.getId());
                receiptNotice.put("conversationId", msg.getConversation().getId());
                receiptNotice.put("status", dto.getType().toUpperCase());
                receiptNotice.put("timestamp", now.toString());
                messagingTemplate.convertAndSend("/topic/user." + msg.getSender().getId() + ".receipts", receiptNotice);
            });
        }
    }

    public List<MessageResponse> getOfflineMessages(UUID recipientId) {
        List<Message> pending = messageRepository.findPendingDeliveriesForRecipient(recipientId);
        return pending.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private MessageResponse mapToResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getRecipient().getId(),
                m.getCiphertext(),
                m.getIv(),
                m.getRatchetPublicKey(),
                m.getSequenceNumber(),
                m.getPreviousCounter(),
                m.getOneTimePreKeyId(),
                m.getStatus(),
                m.isDelivered(),
                m.isRead(),
                m.getCreatedAt()
        );
    }
}

