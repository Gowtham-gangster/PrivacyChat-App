package com.privychat.backend.service;

import com.privychat.backend.dto.auth.AuthResponse;
import com.privychat.backend.dto.auth.ForgotPasswordRequest;
import com.privychat.backend.dto.auth.LoginRequest;
import com.privychat.backend.dto.auth.RegisterRequest;
import com.privychat.backend.dto.auth.ResetPasswordRequest;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserDevice;
import com.privychat.backend.entity.UserPrivacySetting;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.UserDeviceRepository;
import com.privychat.backend.repository.UserPrivacySettingRepository;
import com.privychat.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserPrivacySettingRepository privacySettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       UserDeviceRepository userDeviceRepository,
                       UserPrivacySettingRepository privacySettingRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       OtpService otpService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.privacySettingRepository = privacySettingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest req) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        String normalizedPhone = req.getPhoneNumber().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("An account with this email address already exists");
        }
        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new BadRequestException("An account with this phone number already exists");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getDisplayName(), normalizedEmail, normalizedPhone, passwordHash);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user = userRepository.save(user);

        UserPrivacySetting privacySetting = new UserPrivacySetting(user);
        privacySettingRepository.save(privacySetting);

        // Generate registration OTP and dispatch via SMTP only
        String emailOtp = otpService.generateAndSaveOtp(user.getEmail(), "EMAIL_VERIFICATION");
        emailService.sendRegistrationOtpEmail(user.getEmail(), user.getDisplayName(), emailOtp);
        log.info("Registration verification email dispatched via SMTP for user {}", user.getId());

        return mapToProfile(user);
    }

    @Transactional
    public void resendRegistrationOtp(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isEmailVerified()) {
                throw new BadRequestException("Email is already verified. You can log in.");
            }
            String emailOtp = otpService.generateAndSaveOtp(user.getEmail(), "EMAIL_VERIFICATION");
            emailService.sendRegistrationOtpEmail(user.getEmail(), user.getDisplayName(), emailOtp);
            log.info("Registration OTP resent via SMTP to: {}", normalizedEmail);
        }
    }

    @Transactional
    public boolean verifyEmailOtp(String email, String otpCode) {
        String normalizedEmail = email.toLowerCase().trim();
        otpService.verifyOtp(normalizedEmail, otpCode.trim(), "EMAIL_VERIFICATION");

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        boolean wasAlreadyVerified = user.isEmailVerified();
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        userRepository.save(user);

        // Dispatch Welcome Email via SMTP strictly once upon initial verification
        if (!wasAlreadyVerified) {
            try {
                emailService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());
            } catch (Exception e) {
                log.warn("Welcome email dispatch failed: {}", e.getMessage());
            }
        }

        log.info("Email verification confirmed via SMTP for user {}", user.getId());
        return true;
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest req) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = otpService.generateAndSaveOtp(user.getEmail(), "PASSWORD_RESET");
            emailService.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), resetToken);
            log.info("Password reset code dispatched via SMTP to user {}", user.getId());
        }

        // Generic response prevents account enumeration
        return "If an account exists for this email address, a password reset code has been sent.";
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        otpService.verifyOtp(normalizedEmail, req.getToken().trim(), "PASSWORD_RESET");

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Account not found"));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Revoke all active sessions on password change
        revokeAllSessions(user.getId());

        // Dispatch confirmation email via SMTP
        try {
            emailService.sendPasswordChangedNotification(user.getEmail(), user.getDisplayName());
        } catch (Exception e) {
            log.warn("Password change confirmation email failed: {}", e.getMessage());
        }

        log.info("Password reset successfully completed for user {}", user.getId());
    }

    @Transactional
    public boolean verifyPhoneOtp(String phoneNumber, String otpCode) {
        // SMS verification is discontinued per SMTP-only policy.
        throw new BadRequestException("SMS verification is discontinued. Phone numbers are verified via your registered email address.");
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String identifier = req.getIdentifier().trim();
        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByPhoneNumber(identifier);

        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid credentials");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Account is inactive or suspended");
        }

        // Register or load device
        String label = req.getDeviceLabel() != null ? req.getDeviceLabel() : "Android Client";
        int regId = secureRandom.nextInt(16380) + 1;
        UserDevice device = new UserDevice(user, label, "PENDING_KEY_REGISTRATION", regId);
        device = userDeviceRepository.save(device);

        String accessToken = jwtService.generateAccessToken(user.getId(), device.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), device.getId());

        return new AuthResponse(accessToken, refreshToken, jwtService.getAccessExpirationSeconds(), mapToProfile(user), device.getId());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        UUID deviceId = jwtService.extractDeviceId(refreshToken);

        if (userId == null || deviceId == null) {
            throw new UnauthorizedException("Malformed token claims");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new UnauthorizedException("Device session expired or revoked"));

        if (!device.isActive()) {
            throw new UnauthorizedException("Device session is revoked");
        }

        device.setLastSeenAt(Instant.now());
        userDeviceRepository.save(device);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), device.getId());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), device.getId());

        return new AuthResponse(newAccessToken, newRefreshToken, jwtService.getAccessExpirationSeconds(), mapToProfile(user), device.getId());
    }

    @Transactional
    public void logout(UUID userId, UUID deviceId) {
        if (deviceId != null) {
            userDeviceRepository.findById(deviceId).ifPresent(d -> {
                if (d.getUser().getId().equals(userId)) {
                    d.setActive(false);
                    userDeviceRepository.save(d);
                }
            });
        }
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsActiveTrue(userId);
        for (UserDevice device : devices) {
            device.setActive(false);
        }
        userDeviceRepository.saveAll(devices);
    }

    private UserProfileResponse mapToProfile(User user) {
        String avatarEndpoint = user.getAvatarUrl() != null ? "/api/v1/profile/avatar/" + user.getId() : null;
        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                avatarEndpoint,
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getPendingEmail(),
                user.getPendingPhone(),
                user.getCreatedAt()
        );
    }
}
