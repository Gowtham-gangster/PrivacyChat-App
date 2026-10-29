package com.privychat.backend.service;

import com.privychat.backend.dto.user.ContactResponse;
import com.privychat.backend.dto.user.PrivacySettingsDto;
import com.privychat.backend.dto.user.UpdateProfileRequest;
import com.privychat.backend.dto.user.UserProfileResponse;
import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserPrivacySetting;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.UserPrivacySettingRepository;
import com.privychat.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPrivacySettingRepository privacySettingRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserPrivacySettingRepository privacySettingRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.privacySettingRepository = privacySettingRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }

        user = userRepository.save(user);
        return mapToProfile(user);
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
                results.add(new ContactResponse(user.getId(), user.getDisplayName(), user.getPhoneNumber(), user.getAvatarUrl(), true));
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

        userRepository.delete(user);
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

