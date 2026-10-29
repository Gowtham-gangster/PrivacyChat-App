package com.privychat.backend.repository;

import com.privychat.backend.entity.CallSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {
    @Query("SELECT c FROM CallSession c WHERE c.caller.id = :userId OR c.callee.id = :userId ORDER BY c.startedAt DESC")
    List<CallSession> findUserCallHistory(@Param("userId") UUID userId, Pageable pageable);
}

