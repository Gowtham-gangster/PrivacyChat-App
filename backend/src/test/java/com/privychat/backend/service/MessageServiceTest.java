package com.privychat.backend.service;

import com.privychat.backend.dto.chat.MessageResponse;
import com.privychat.backend.dto.chat.SendMessageRequest;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.Message;
import com.privychat.backend.entity.User;
import com.privychat.backend.repository.BlockedUserRepository;
import com.privychat.backend.repository.MessageRepository;
import com.privychat.backend.repository.UserDeviceRepository;
import com.privychat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository,
                conversationService,
                userRepository,
                userDeviceRepository,
                blockedUserRepository,
                messagingTemplate
        );
    }

    @Test
    void testSendMessageSuccess() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User sender = new User("Alice", "alice@example.com", "+12025550143", "pass");
        sender.setId(senderId);
        User recipient = new User("Bob", "bob@example.com", "+12025550199", "pass");
        recipient.setId(recipientId);

        Conversation conv = new Conversation(sender, recipient);
        conv.setId(convId);

        SendMessageRequest req = new SendMessageRequest(
                messageId,
                convId,
                recipientId,
                "ciphertext_base64_blob",
                "iv_base64_blob",
                "ratchet_pub_key",
                1L
        );

        when(conversationService.getAuthorizedConversation(convId, senderId)).thenReturn(conv);
        when(blockedUserRepository.isBlockedEitherWay(senderId, recipientId)).thenReturn(false);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(senderId)).thenReturn(sender);
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        Message savedMsg = new Message(messageId, conv, sender, recipient, req.getCiphertext(), req.getIv(), req.getRatchetPublicKey(), 1L);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        MessageResponse response = messageService.sendMessage(senderId, req);

        assertNotNull(response);
        assertEquals(messageId, response.getId());
        assertEquals("ciphertext_base64_blob", response.getCiphertext());

        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user." + recipientId + ".messages"), any(MessageResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user." + senderId + ".receipts"), any(Object.class));
    }

    @Test
    void testSendMessageDeduplication() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User sender = new User("Alice", "alice@example.com", "+12025550143", "pass");
        sender.setId(senderId);
        User recipient = new User("Bob", "bob@example.com", "+12025550199", "pass");
        recipient.setId(recipientId);

        Conversation conv = new Conversation(sender, recipient);
        conv.setId(convId);

        SendMessageRequest req = new SendMessageRequest(
                messageId,
                convId,
                recipientId,
                "ciphertext_base64_blob",
                "iv_base64_blob",
                "ratchet_pub_key",
                1L
        );

        Message existingMsg = new Message(messageId, conv, sender, recipient, req.getCiphertext(), req.getIv(), req.getRatchetPublicKey(), 1L);

        when(conversationService.getAuthorizedConversation(convId, senderId)).thenReturn(conv);
        when(blockedUserRepository.isBlockedEitherWay(senderId, recipientId)).thenReturn(false);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existingMsg));

        MessageResponse response = messageService.sendMessage(senderId, req);

        assertNotNull(response);
        assertEquals(messageId, response.getId());
        verify(messageRepository, never()).save(any());
    }
}

