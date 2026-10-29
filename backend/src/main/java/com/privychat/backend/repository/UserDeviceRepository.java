package com.privychat.backend.repository;

import com.privychat.backend.entity.User;
import com.privychat.backend.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
    List<UserDevice> findByUserAndIsActiveTrue(User user);
    Optional<UserDevice> findByUserAndRegistrationId(User user, int registrationId);
    Optional<UserDevice> findByIdAndUser(UUID id, User user);
    List<UserDevice> findByUserIdAndIsActiveTrue(UUID userId);
}

