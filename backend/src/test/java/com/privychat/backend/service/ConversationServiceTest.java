package com.privychat.backend.service;

import com.privychat.backend.dto.chat.ConversationResponse;
import com.privychat.backend.entity.Conversation;
import com.privychat.backend.entity.User;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private BlockedUserRepository blockedUserRepository;

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(
                conversationRepository,
                participantRepository,
                userRepository,
                messageRepository,
                blockedUserRepository
        );
    }

    @Test
    void testGetOrCreateDirectConversationSuccess() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        User alice = new User("Alice", "alice@example.com", "+12025550143", "pass");
        alice.setId(aliceId);
        User bob = new User("Bob", "bob@example.com", "+12025550199", "pass");
        bob.setId(bobId);

        when(blockedUserRepository.isBlockedEitherWay(aliceId, bobId)).thenReturn(false);
        when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(conversationRepository.findBetweenUsers(alice, bob)).thenReturn(Optional.empty());

        Conversation conv = new Conversation(alice, bob);
        conv.setId(UUID.randomUUID());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

        ConversationResponse response = conversationService.getOrCreateDirectConversation(aliceId, bobId);

        assertNotNull(response);
        assertEquals(bobId, response.getParticipant().getId());
        assertEquals("Bob", response.getParticipant().getDisplayName());
        verify(participantRepository, times(2)).save(any());
    }

    @Test
    void testCannotStartConversationWithSelf() {
        UUID aliceId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> conversationService.getOrCreateDirectConversation(aliceId, aliceId));
    }

    @Test
    void testCannotStartConversationWithBlockedUser() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        when(blockedUserRepository.isBlockedEitherWay(aliceId, bobId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> conversationService.getOrCreateDirectConversation(aliceId, bobId));
    }
}

