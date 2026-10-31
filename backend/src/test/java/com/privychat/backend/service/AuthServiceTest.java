package com.privychat.backend.service;

import com.privychat.backend.dto.auth.*;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserDevice;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.UserDeviceRepository;
import com.privychat.backend.repository.UserPrivacySettingRepository;
import com.privychat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private UserPrivacySettingRepository privacySettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                userDeviceRepository,
                privacySettingRepository,
                passwordEncoder,
                jwtService,
                otpService,
                emailService
        );
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "+12025550143", "Password123!", true);

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+12025550143")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pass");

        User savedUser = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(otpService.generateAndSaveOtp(eq("alice@example.com"), eq("EMAIL_VERIFICATION"))).thenReturn("123456");

        UserProfileResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("Alice", response.getDisplayName());
        assertEquals("alice@example.com", response.getEmail());
        verify(emailService).sendRegistrationOtpEmail(eq("alice@example.com"), eq("Alice"), eq("123456"));
    }

    @Test
    void testRegisterDuplicateEmailThrowsBadRequest() {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "+12025550143", "Password123!", true);
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void testVerifyEmailOtpSuccess_dispatchesWelcomeEmailOnce() {
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        user.setEmailVerified(false);
        when(otpService.verifyOtp("alice@example.com", "123456", "EMAIL_VERIFICATION")).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        boolean result = authService.verifyEmailOtp("alice@example.com", "123456");

        assertTrue(result);
        assertTrue(user.isEmailVerified());
        assertTrue(user.isPhoneVerified());
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail("alice@example.com", "Alice");
    }

    @Test
    void testVerifyEmailOtpRetry_doesNotSendDuplicateWelcomeEmail() {
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        user.setEmailVerified(true); // already verified previously
        when(otpService.verifyOtp("alice@example.com", "123456", "EMAIL_VERIFICATION")).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        boolean result = authService.verifyEmailOtp("alice@example.com", "123456");

        assertTrue(result);
        verify(userRepository).save(user);
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void testResendRegistrationOtp() {
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateAndSaveOtp("alice@example.com", "EMAIL_VERIFICATION")).thenReturn("654321");

        authService.resendRegistrationOtp("alice@example.com");

        verify(emailService).sendRegistrationOtpEmail("alice@example.com", "Alice", "654321");
    }

    @Test
    void testForgotPassword_accountExists_dispatchesResetEmail() {
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(otpService.generateAndSaveOtp("alice@example.com", "PASSWORD_RESET")).thenReturn("998877");

        String msg = authService.forgotPassword(new ForgotPasswordRequest("alice@example.com"));

        assertNotNull(msg);
        verify(emailService).sendPasswordResetEmail("alice@example.com", "Alice", "998877");
    }

    @Test
    void testForgotPassword_accountDoesNotExist_returnsGenericMessageWithoutEmail() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        String msg = authService.forgotPassword(new ForgotPasswordRequest("unknown@example.com"));

        assertNotNull(msg);
        verifyNoInteractions(emailService);
    }

    @Test
    void testResetPassword_success() {
        User user = new User("Alice", "alice@example.com", "+12025550143", "old_hash");
        user.setId(UUID.randomUUID());
        when(otpService.verifyOtp("alice@example.com", "998877", "PASSWORD_RESET")).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("new_hash");
        when(userDeviceRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(Collections.emptyList());

        authService.resetPassword(new ResetPasswordRequest("alice@example.com", "998877", "NewSecurePassword123!"));

        assertEquals("new_hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(emailService).sendPasswordChangedNotification("alice@example.com", "Alice");
    }

    @Test
    void testVerifyPhoneOtp_discontinuedThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> authService.verifyPhoneOtp("+12025550143", "123456"));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = new LoginRequest("alice@example.com", "Password123!", "Pixel 8 Pro");
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");
        user.setId(UUID.randomUUID());

        UserDevice device = new UserDevice(user, "Pixel 8 Pro", "PENDING", 1234);
        device.setId(UUID.randomUUID());

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed_pass")).thenReturn(true);
        when(userDeviceRepository.save(any(UserDevice.class))).thenReturn(device);
        when(jwtService.generateAccessToken(eq(user.getId()), eq(device.getId()))).thenReturn("mock_access_token");
        when(jwtService.generateRefreshToken(eq(user.getId()), eq(device.getId()))).thenReturn("mock_refresh_token");
        when(jwtService.getAccessExpirationSeconds()).thenReturn(900L);

        AuthResponse authResponse = authService.login(req);

        assertNotNull(authResponse);
        assertEquals("mock_access_token", authResponse.getAccessToken());
        assertEquals("mock_refresh_token", authResponse.getRefreshToken());
        assertEquals(device.getId(), authResponse.getDeviceId());
    }

    @Test
    void testLoginInvalidPasswordThrowsUnauthorized() {
        LoginRequest req = new LoginRequest("alice@example.com", "WrongPassword", "Pixel 8 Pro");
        User user = new User("Alice", "alice@example.com", "+12025550143", "hashed_pass");

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_pass")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(req));
    }
}
