package com.privychat.backend.repository;

import com.privychat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByEmailIgnoreCaseOrPhoneNumber(String email, String phoneNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByPhoneNumberIn(List<String> phoneNumbers);
}

