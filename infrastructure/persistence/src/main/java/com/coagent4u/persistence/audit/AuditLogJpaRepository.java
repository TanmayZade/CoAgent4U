package com.coagent4u.persistence.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    Page<AuditLogJpaEntity> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);

    Page<AuditLogJpaEntity> findByUserIdAndEventTypeOrderByOccurredAtDesc(
            UUID userId, String eventType, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndEventType(UUID userId, String eventType);

    List<AuditLogJpaEntity> findByUserIdOrderByOccurredAtDesc(UUID userId);
}
