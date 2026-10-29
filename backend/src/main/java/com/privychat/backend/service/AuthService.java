package com.privychat.backend.service;

import com.privychat.backend.dto.auth.AuthResponse;
import com.privychat.backend.dto.auth.LoginRequest;
import com.privychat.backend.dto.auth.RegisterRequest;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserDevice;
import com.privychat.backend.entity.UserPrivacySetting;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.UserDeviceRepository;
import com.privychat.backend.repository.UserPrivacySettingRepository;
import com.privychat.backend.repository.UserRepository;
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
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new BadRequestException("An account with this email address already exists");
        }
        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new BadRequestException("An account with this phone number already exists");
        }

        String passwordHash = passwordEncoder.encode(req.getPassword());
        User user = new User(req.getDisplayName(), req.getEmail().toLowerCase().trim(), req.getPhoneNumber().trim(), passwordHash);
        user = userRepository.save(user);

        UserPrivacySetting privacySetting = new UserPrivacySetting(user);
        privacySettingRepository.save(privacySetting);

        // Generate and dispatch verification OTPs
        String emailOtp = otpService.generateAndSaveOtp(user.getEmail(), "EMAIL_VERIFICATION");
        emailService.sendVerificationOtpEmail(user.getEmail(), emailOtp);

        String phoneOtp = otpService.generateAndSaveOtp(user.getPhoneNumber(), "PHONE_VERIFICATION");
        // In local/dev environments or SMS gateways, the OTP is generated and logged/dispatched
        org.slf4j.LoggerFactory.getLogger(AuthService.class).info("Verification OTP for phone {}: {}", user.getPhoneNumber(), phoneOtp);

        return mapToProfile(user);
    }

    @Transactional
    public boolean verifyEmailOtp(String email, String otpCode) {
        otpService.verifyOtp(email.toLowerCase().trim(), otpCode, "EMAIL_VERIFICATION");
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean verifyPhoneOtp(String phoneNumber, String otpCode) {
        otpService.verifyOtp(phoneNumber.trim(), otpCode, "PHONE_VERIFICATION");
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setPhoneVerified(true);
        userRepository.save(user);
        return true;
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
        return new UserProfileResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.isPhoneVerified()
        );
    }
}

