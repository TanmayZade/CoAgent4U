package com.coagent4u.persistence.activity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

@Repository
public interface AgentActivityJpaRepository extends JpaRepository<AgentActivityJpaEntity, UUID> {
    @Query("SELECT a FROM AgentActivityJpaEntity a WHERE a.agentId = :agentId " +
           "AND (:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:level IS NULL OR a.level = :level) " +
           "AND (cast(:startDate as timestamp) IS NULL OR a.occurredAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR a.occurredAt <= :endDate) " +
           "ORDER BY a.occurredAt DESC")
    Page<AgentActivityJpaEntity> findFiltered(
            @Param("agentId") UUID agentId,
            @Param("eventType") String eventType,
            @Param("level") String level,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM AgentActivityJpaEntity a WHERE a.agentId = :agentId " +
           "AND (:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:level IS NULL OR a.level = :level) " +
           "AND (cast(:startDate as timestamp) IS NULL OR a.occurredAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR a.occurredAt <= :endDate)")
    long countFiltered(
            @Param("agentId") UUID agentId,
            @Param("eventType") String eventType,
            @Param("level") String level,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    List<AgentActivityJpaEntity> findByAgentIdOrderByOccurredAtDesc(UUID agentId);
}
