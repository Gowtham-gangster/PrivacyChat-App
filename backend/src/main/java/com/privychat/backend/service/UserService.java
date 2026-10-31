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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final UserRepository userRepository;
    private final UserPrivacySettingRepository privacySettingRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SupabaseStorageService supabaseStorageService;

    public UserService(UserRepository userRepository,
                       UserPrivacySettingRepository privacySettingRepository,
                       ConversationParticipantRepository conversationParticipantRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       EmailService emailService,
                       SupabaseStorageService supabaseStorageService) {
        this.userRepository = userRepository;
        this.privacySettingRepository = privacySettingRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.supabaseStorageService = supabaseStorageService;
    }

    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (req.getDisplayName() != null && !req.getDisplayName().trim().isEmpty()) {
            user.setDisplayName(req.getDisplayName().trim());
        }

        user = userRepository.save(user);
        return mapToProfile(user);
    }

    @Transactional
    public void requestEmailChange(UUID userId, EmailChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String normalizedEmail = req.getNewEmail().trim().toLowerCase();

        if (normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("New email address must be different from your current email");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("This email address is already associated with another account");
        }

        // Store pending email on user record
        user.setPendingEmail(normalizedEmail);
        userRepository.save(user);

        // Generate single-use verification code with 10 minute expiration
        String code = otpService.generateAndSaveOtp(user.getId().toString() + ":" + normalizedEmail, "EMAIL_CHANGE");

        // Send confirmation email to the new address
        emailService.sendEmailChangeVerification(normalizedEmail, code);
        log.info("Email change verification dispatched for user {}", userId);
    }

    @Transactional
    public UserProfileResponse verifyEmailChange(UUID userId, VerifyEmailChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String pendingEmail = user.getPendingEmail();
        if (pendingEmail == null || pendingEmail.isBlank()) {
            throw new BadRequestException("No pending email change request found for this account");
        }

        // Verify single-use OTP
        otpService.verifyOtp(user.getId().toString() + ":" + pendingEmail, req.getToken().trim(), "EMAIL_CHANGE");

        // Check uniqueness again to prevent race conditions
        if (userRepository.existsByEmailIgnoreCase(pendingEmail)) {
            user.setPendingEmail(null);
            userRepository.save(user);
            throw new BadRequestException("This email address was claimed by another account");
        }

        String oldEmail = user.getEmail();
        user.setEmail(pendingEmail);
        user.setEmailVerified(true);
        user.setPendingEmail(null);
        user = userRepository.save(user);

        // Security notification to the old email
        emailService.sendEmailChangedNotification(oldEmail, pendingEmail);
        log.info("Email address successfully changed for user {}", userId);

        return mapToProfile(user);
    }

    @Transactional
    public void requestPhoneChange(UUID userId, PhoneChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String normalizedPhone = req.getNewPhoneNumber().trim();

        if (normalizedPhone.equals(user.getPhoneNumber())) {
            throw new BadRequestException("New phone number must be different from your current number");
        }

        if (userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new BadRequestException("This phone number is already registered to another account");
        }

        user.setPendingPhone(normalizedPhone);
        userRepository.save(user);

        // Generate 6-digit verification code bound to user and new phone
        String code = otpService.generateAndSaveOtp(user.getId().toString() + ":" + normalizedPhone, "PHONE_CHANGE");

        // Mask phone number for security display in email (e.g. +141 *** *** 0299)
        String maskedPhone = maskPhoneNumber(normalizedPhone);

        // Send verification code exclusively to user's registered email address via SMTP
        emailService.sendPhoneChangeVerificationEmail(user.getEmail(), user.getDisplayName(), maskedPhone, code);
        log.info("Phone change verification code dispatched via SMTP to user {}", userId);
    }

    @Transactional
    public UserProfileResponse verifyPhoneChange(UUID userId, VerifyPhoneChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String pendingPhone = user.getPendingPhone();
        if (pendingPhone == null || pendingPhone.isBlank()) {
            throw new BadRequestException("No pending phone change request found for this account");
        }

        // Verify OTP
        otpService.verifyOtp(user.getId().toString() + ":" + pendingPhone, req.getCode().trim(), "PHONE_CHANGE");

        if (userRepository.existsByPhoneNumber(pendingPhone)) {
            user.setPendingPhone(null);
            userRepository.save(user);
            throw new BadRequestException("This phone number was claimed by another account");
        }

        user.setPhoneNumber(pendingPhone);
        user.setPhoneVerified(true);
        user.setPendingPhone(null);
        user = userRepository.save(user);

        // Security notification to the user's verified email via SMTP
        String maskedPhone = maskPhoneNumber(user.getPhoneNumber());
        try {
            emailService.sendPhoneChangedNotification(user.getEmail(), user.getDisplayName(), maskedPhone);
        } catch (Exception e) {
            log.warn("Phone change security notification email failed: {}", e.getMessage());
        }

        log.info("Phone number successfully updated and verified via email for user {}", userId);
        return mapToProfile(user);
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        int len = phone.length();
        String prefix = phone.substring(0, Math.min(5, len - 4));
        String suffix = phone.substring(len - 4);
        return prefix + " *** *** " + suffix;
    }

    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) throws IOException, InterruptedException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar image file cannot be empty");
        }

        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Avatar image exceeds maximum allowed size of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg") && !contentType.equals("image/webp"))) {
            throw new BadRequestException("Unsupported image format. Allowed formats: PNG, JPEG, WEBP");
        }

        // Clean up previous avatar if exists in Supabase Storage
        String oldAvatarPath = user.getAvatarUrl();
        if (oldAvatarPath != null && oldAvatarPath.startsWith("avatars/")) {
            try {
                supabaseStorageService.deleteObject(oldAvatarPath);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar object {}: {}", oldAvatarPath, e.getMessage());
            }
        }

        // Upload new avatar with secure UUID filename
        String ext = contentType.equals("image/jpeg") ? ".jpg" : (contentType.equals("image/webp") ? ".webp" : ".png");
        String objectPath = "avatars/" + userId + "/" + UUID.randomUUID() + ext;

        supabaseStorageService.uploadCiphertext(objectPath, file.getBytes(), contentType);

        user.setAvatarUrl(objectPath);
        user = userRepository.save(user);

        log.info("Avatar uploaded successfully for user {} to path {}", userId, objectPath);
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileResponse deleteAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String oldAvatarPath = user.getAvatarUrl();
        if (oldAvatarPath != null && oldAvatarPath.startsWith("avatars/")) {
            try {
                supabaseStorageService.deleteObject(oldAvatarPath);
            } catch (Exception e) {
                log.warn("Failed to delete avatar object {}: {}", oldAvatarPath, e.getMessage());
            }
        }

        user.setAvatarUrl(null);
        user = userRepository.save(user);

        log.info("Avatar removed for user {}", userId);
        return mapToProfile(user);
    }

    public byte[] getAvatarBytes(UUID requesterId, UUID targetUserId) throws IOException, InterruptedException {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        String avatarPath = targetUser.getAvatarUrl();
        if (avatarPath == null || avatarPath.isBlank()) {
            throw new ResourceNotFoundException("User has no avatar set");
        }

        // Enforce privacy restrictions
        if (!requesterId.equals(targetUserId)) {
            UserPrivacySetting privacy = privacySettingRepository.findById(targetUserId).orElse(null);
            if (privacy != null) {
                String visibility = privacy.getProfilePhotoVisibility();
                if ("NOBODY".equalsIgnoreCase(visibility)) {
                    throw new UnauthorizedException("User privacy settings do not permit viewing profile photo");
                }
                if ("CONTACTS".equalsIgnoreCase(visibility)) {
                    boolean isContact = conversationParticipantRepository.findByUserId(requesterId).stream()
                            .anyMatch(cp -> conversationParticipantRepository.findByConversationIdAndUserId(cp.getConversation().getId(), targetUserId).isPresent());
                    if (!isContact) {
                        throw new UnauthorizedException("Only contacts may view this user's profile photo");
                    }
                }
            }
        }

        return supabaseStorageService.downloadCiphertext(avatarPath);
    }

    public PrivacySettingsDto getPrivacySettings(UUID userId) {
        UserPrivacySetting settings = privacySettingRepository.findById(userId)
                .orElseGet(() -> new UserPrivacySetting(userRepository.getReferenceById(userId)));
        return new PrivacySettingsDto(
                settings.isReadReceiptsEnabled(),
                settings.getLastSeenVisibility(),
                settings.getProfilePhotoVisibility()
        );
    }

    @Transactional
    public PrivacySettingsDto updatePrivacySettings(UUID userId, PrivacySettingsDto dto) {
        UserPrivacySetting settings = privacySettingRepository.findById(userId)
                .orElseGet(() -> new UserPrivacySetting(userRepository.getReferenceById(userId)));

        settings.setReadReceiptsEnabled(dto.isReadReceiptsEnabled());
        if (dto.getLastSeenVisibility() != null) {
            settings.setLastSeenVisibility(dto.getLastSeenVisibility());
        }
        if (dto.getProfilePhotoVisibility() != null) {
            settings.setProfilePhotoVisibility(dto.getProfilePhotoVisibility());
        }

        settings = privacySettingRepository.save(settings);
        return new PrivacySettingsDto(
                settings.isReadReceiptsEnabled(),
                settings.getLastSeenVisibility(),
                settings.getProfilePhotoVisibility()
        );
    }

    public List<ContactResponse> discoverContacts(UUID currentUserId, List<String> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalized = phoneNumbers.stream()
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        List<User> registeredUsers = userRepository.findByPhoneNumberIn(normalized);
        Map<String, User> userMap = registeredUsers.stream()
                .collect(Collectors.toMap(User::getPhoneNumber, u -> u));

        List<ContactResponse> results = new ArrayList<>();
        for (String phone : normalized) {
            User user = userMap.get(phone);
            if (user != null && !user.getId().equals(currentUserId)) {
                String avatarApi = user.getAvatarUrl() != null ? "/api/v1/profile/avatar/" + user.getId() : null;
                results.add(new ContactResponse(user.getId(), user.getDisplayName(), user.getPhoneNumber(), avatarApi, true));
            } else if (user == null) {
                results.add(new ContactResponse(null, null, phone, null, false));
            }
        }
        return results;
    }

    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password for account deletion");
        }

        // Clean up avatar if present
        if (user.getAvatarUrl() != null && user.getAvatarUrl().startsWith("avatars/")) {
            try {
                supabaseStorageService.deleteObject(user.getAvatarUrl());
            } catch (Exception ignored) {}
        }

        userRepository.delete(user);
        log.info("User account {} deleted successfully", userId);
    }

    public Map<String, Object> exportUserData(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserPrivacySetting settings = privacySettingRepository.findById(userId).orElse(null);

        Map<String, Object> export = new HashMap<>();
        export.put("id", user.getId());
        export.put("displayName", user.getDisplayName());
        export.put("email", user.getEmail());
        export.put("phoneNumber", user.getPhoneNumber());
        export.put("createdAt", user.getCreatedAt().toString());
        if (settings != null) {
            export.put("readReceiptsEnabled", settings.isReadReceiptsEnabled());
            export.put("lastSeenVisibility", settings.getLastSeenVisibility());
        }
        return export;
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
