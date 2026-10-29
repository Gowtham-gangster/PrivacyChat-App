package com.privychat.backend.service;

import com.privychat.backend.dto.keys.OneTimePreKeyDto;
import com.privychat.backend.dto.keys.PreKeyBundleResponse;
import com.privychat.backend.dto.keys.PublishKeysRequest;
import com.privychat.backend.dto.keys.SignedPreKeyDto;
import com.privychat.backend.entity.DevicePreKeyBundle;
import com.privychat.backend.entity.OneTimePreKey;
import com.privychat.backend.entity.UserDevice;
import com.privychat.backend.exception.ResourceNotFoundException;
import com.privychat.backend.exception.UnauthorizedException;
import com.privychat.backend.repository.DevicePreKeyBundleRepository;
import com.privychat.backend.repository.OneTimePreKeyRepository;
import com.privychat.backend.repository.UserDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceKeyService {

    private final UserDeviceRepository userDeviceRepository;
    private final DevicePreKeyBundleRepository signedPreKeyRepository;
    private final OneTimePreKeyRepository oneTimePreKeyRepository;

    public DeviceKeyService(UserDeviceRepository userDeviceRepository,
                            DevicePreKeyBundleRepository signedPreKeyRepository,
                            OneTimePreKeyRepository oneTimePreKeyRepository) {
        this.userDeviceRepository = userDeviceRepository;
        this.signedPreKeyRepository = signedPreKeyRepository;
        this.oneTimePreKeyRepository = oneTimePreKeyRepository;
    }

    @Transactional
    public void publishKeys(UUID userId, PublishKeysRequest req) {
        UserDevice device = userDeviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + req.getDeviceId()));

        if (!device.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Cannot publish keys for another user's device");
        }

        // Update device identity key and registration ID
        device.setPublicIdentityKey(req.getIdentityKey());
        if (req.getRegistrationId() > 0) {
            device.setRegistrationId(req.getRegistrationId());
        }
        userDeviceRepository.save(device);

        // Save signed prekey
        if (req.getSignedPreKey() != null) {
            SignedPreKeyDto spk = req.getSignedPreKey();
            DevicePreKeyBundle bundle = new DevicePreKeyBundle(
                    device,
                    spk.getKeyId(),
                    spk.getPublicKey(),
                    spk.getSignature()
            );
            signedPreKeyRepository.save(bundle);
        }

        // Save one-time prekeys
        if (req.getOneTimePreKeys() != null && !req.getOneTimePreKeys().isEmpty()) {
            List<OneTimePreKey> otpkList = new ArrayList<>();
            for (OneTimePreKeyDto otpkDto : req.getOneTimePreKeys()) {
                otpkList.add(new OneTimePreKey(device, otpkDto.getKeyId(), otpkDto.getPublicKey()));
            }
            oneTimePreKeyRepository.saveAll(otpkList);
        }
    }

    @Transactional
    public PreKeyBundleResponse getPreKeyBundle(UUID recipientUserId) {
        List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsActiveTrue(recipientUserId);
        if (devices.isEmpty()) {
            throw new ResourceNotFoundException("No active device found for recipient: " + recipientUserId);
        }

        // Use the primary/first active device
        UserDevice device = devices.get(0);

        DevicePreKeyBundle signedPreKey = signedPreKeyRepository.findTopByDeviceOrderByCreatedAtDesc(device)
                .orElseThrow(() -> new ResourceNotFoundException("No signed prekey found for device: " + device.getId()));

        // Consume an available one-time prekey
        Optional<OneTimePreKey> otpkOpt = oneTimePreKeyRepository.findFirstAvailableByDevice(device);
        OneTimePreKeyDto otpkDto = null;
        if (otpkOpt.isPresent()) {
            OneTimePreKey otpk = otpkOpt.get();
            otpk.setConsumed(true);
            otpk.setConsumedAt(Instant.now());
            oneTimePreKeyRepository.save(otpk);
            otpkDto = new OneTimePreKeyDto(otpk.getKeyId(), otpk.getPublicKey());
        }

        return new PreKeyBundleResponse(
                recipientUserId,
                device.getId(),
                device.getRegistrationId(),
                device.getPublicIdentityKey(),
                new SignedPreKeyDto(signedPreKey.getSignedPreKeyId(), signedPreKey.getSignedPreKeyPublic(), signedPreKey.getSignedPreKeySignature()),
                otpkDto
        );
    }
}

