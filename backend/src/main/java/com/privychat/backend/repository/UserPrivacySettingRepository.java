package com.privychat.backend.repository;

import com.privychat.backend.entity.UserPrivacySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserPrivacySettingRepository extends JpaRepository<UserPrivacySetting, UUID> {
}

