package com.privychat.backend.repository;

import com.privychat.backend.entity.DevicePreKeyBundle;
import com.privychat.backend.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicePreKeyBundleRepository extends JpaRepository<DevicePreKeyBundle, UUID> {
    Optional<DevicePreKeyBundle> findTopByDeviceOrderByCreatedAtDesc(UserDevice device);
    Optional<DevicePreKeyBundle> findByDeviceAndSignedPreKeyId(UserDevice device, int signedPreKeyId);
}

