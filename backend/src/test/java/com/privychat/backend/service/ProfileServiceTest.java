package com.privychat.backend.service;

import com.privychat.backend.dto.user.*;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserPrivacySetting;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.ConversationParticipantRepository;
import com.privychat.backend.repository.UserPrivacySettingRepository;
import com.privychat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPrivacySettingRepository privacySettingRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private UserService userService;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                privacySettingRepository,
                conversationParticipantRepository,
                passwordEncoder,
                otpService,
                emailService,
                supabaseStorageService
        );

        userId = UUID.randomUUID();
        testUser = new User("Alice Doe", "alice@example.com", "+14155550100", "hashed_pass");
        testUser.setId(userId);
    }

    @Test
    void testGetProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getProfile(userId);

        assertNotNull(response);
        assertEquals("Alice Doe", response.getDisplayName());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("+14155550100", response.getPhoneNumber());
    }

    @Test
    void testGetProfile_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(userId));
    }

    @Test
    void testUpdateDisplayName_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest("Alice Updated", null);
        UserProfileResponse response = userService.updateProfile(userId, req);

        assertEquals("Alice Updated", response.getDisplayName());
        verify(userRepository).save(testUser);
    }

    @Test
    void testRequestEmailChange_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailIgnoreCase("newalice@example.com")).thenReturn(false);
        when(otpService.generateAndSaveOtp(anyString(), eq("EMAIL_CHANGE"))).thenReturn("654321");

        userService.requestEmailChange(userId, new EmailChangeRequest("newalice@example.com"));

        assertEquals("newalice@example.com", testUser.getPendingEmail());
        verify(emailService).sendEmailChangeVerification("newalice@example.com", "654321");
        verify(userRepository).save(testUser);
    }

    @Test
    void testRequestEmailChange_SameEmailFails() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () ->
                userService.requestEmailChange(userId, new EmailChangeRequest("alice@example.com")));
    }

    @Test
    void testRequestEmailChange_DuplicateEmailFails() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailIgnoreCase("other@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                userService.requestEmailChange(userId, new EmailChangeRequest("other@example.com")));
    }

    @Test
    void testVerifyEmailChange_Success() {
        testUser.setPendingEmail("newalice@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(otpService.verifyOtp(contains("newalice@example.com"), eq("654321"), eq("EMAIL_CHANGE"))).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("newalice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = userService.verifyEmailChange(userId, new VerifyEmailChangeRequest("654321"));

        assertEquals("newalice@example.com", response.getEmail());
        assertTrue(response.isEmailVerified());
        assertNull(testUser.getPendingEmail());
        verify(emailService).sendEmailChangedNotification("alice@example.com", "newalice@example.com");
    }

    @Test
    void testVerifyEmailChange_NoPendingRequestFails() {
        testUser.setPendingEmail(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () ->
                userService.verifyEmailChange(userId, new VerifyEmailChangeRequest("654321")));
    }

    @Test
    void testRequestPhoneChange_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByPhoneNumber("+14155550299")).thenReturn(false);
        when(otpService.generateAndSaveOtp(anyString(), eq("PHONE_CHANGE"))).thenReturn("112233");

        userService.requestPhoneChange(userId, new PhoneChangeRequest("+14155550299"));

        assertEquals("+14155550299", testUser.getPendingPhone());
        verify(emailService).sendPhoneChangeVerificationEmail(eq("alice@example.com"), eq("Alice Doe"), contains("0299"), eq("112233"));
        verify(userRepository).save(testUser);
    }

    @Test
    void testVerifyPhoneChange_Success() {
        testUser.setPendingPhone("+14155550299");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(otpService.verifyOtp(contains("+14155550299"), eq("112233"), eq("PHONE_CHANGE"))).thenReturn(true);
        when(userRepository.existsByPhoneNumber("+14155550299")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = userService.verifyPhoneChange(userId, new VerifyPhoneChangeRequest("112233"));

        assertEquals("+14155550299", response.getPhoneNumber());
        assertTrue(response.isPhoneVerified());
        assertNull(testUser.getPendingPhone());
        verify(emailService).sendPhoneChangedNotification(eq("alice@example.com"), eq("Alice Doe"), contains("0299"));
    }

    @Test
    void testUploadAvatar_Success() throws IOException, InterruptedException {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});
        UserProfileResponse response = userService.uploadAvatar(userId, file);

        assertNotNull(response);
        verify(supabaseStorageService).uploadCiphertext(contains("avatars/" + userId), eq(new byte[]{1, 2, 3, 4}), eq("image/png"));
        assertTrue(testUser.getAvatarUrl().startsWith("avatars/" + userId));
    }

    @Test
    void testUploadAvatar_OversizedFails() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        byte[] bigData = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", bigData);

        assertThrows(BadRequestException.class, () -> userService.uploadAvatar(userId, file));
    }

    @Test
    void testDeleteAvatar_Success() {
        testUser.setAvatarUrl("avatars/" + userId + "/some-uuid.png");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = userService.deleteAvatar(userId);

        assertNull(response.getAvatarUrl());
        assertNull(testUser.getAvatarUrl());
        verify(supabaseStorageService).deleteObject("avatars/" + userId + "/some-uuid.png");
    }

    @Test
    void testGetAvatar_PrivacyRestrictedNobody() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User("Bob", "bob@example.com", "+14155550300", "hash");
        otherUser.setId(otherUserId);
        otherUser.setAvatarUrl("avatars/" + otherUserId + "/avatar.png");

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        UserPrivacySetting privacy = new UserPrivacySetting(otherUser);
        privacy.setProfilePhotoVisibility("NOBODY");
        when(privacySettingRepository.findById(otherUserId)).thenReturn(Optional.of(privacy));

        assertThrows(UnauthorizedException.class, () -> userService.getAvatarBytes(userId, otherUserId));
    }
}

