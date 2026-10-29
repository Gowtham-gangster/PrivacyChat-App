package com.privychat.backend.repository;

import com.privychat.backend.entity.BlockedUser;
import com.privychat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, UUID> {
    Optional<BlockedUser> findByBlockerAndBlocked(User blocker, User blocked);
    List<BlockedUser> findByBlocker(User blocker);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BlockedUser b WHERE (b.blocker.id = :u1 AND b.blocked.id = :u2) OR (b.blocker.id = :u2 AND b.blocked.id = :u1)")
    boolean isBlockedEitherWay(@Param("u1") UUID u1, @Param("u2") UUID u2);
}

