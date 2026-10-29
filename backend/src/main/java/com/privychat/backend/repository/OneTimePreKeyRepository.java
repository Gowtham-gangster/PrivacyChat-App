package com.privychat.backend.repository;

import com.privychat.backend.entity.OneTimePreKey;
import com.privychat.backend.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OneTimePreKeyRepository extends JpaRepository<OneTimePreKey, UUID> {
    
    @Query("SELECT k FROM OneTimePreKey k WHERE k.device = :device AND k.consumed = false ORDER BY k.createdAt ASC LIMIT 1")
    Optional<OneTimePreKey> findFirstAvailableByDevice(@Param("device") UserDevice device);

    long countByDeviceAndConsumedFalse(UserDevice device);

    Optional<OneTimePreKey> findByDeviceAndKeyId(UserDevice device, int keyId);
}

