package com.coagent4u.persistence.coordination;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinationJpaRepository extends JpaRepository<CoordinationJpaEntity, UUID> {

    Page<CoordinationJpaEntity> findByRequesterAgentIdOrInviteeAgentIdOrderByCreatedAtDesc(
            UUID requesterAgentId, UUID inviteeAgentId, Pageable pageable);

    long countByRequesterAgentIdOrInviteeAgentId(UUID requesterAgentId, UUID inviteeAgentId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CoordinationJpaEntity c WHERE (c.requesterAgentId = :agentId OR c.inviteeAgentId = :agentId) AND c.state IN :states ORDER BY c.createdAt DESC")
    Page<CoordinationJpaEntity> findByAgentIdAndStates(@org.springframework.data.repository.query.Param("agentId") UUID agentId, @org.springframework.data.repository.query.Param("states") List<String> states, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM CoordinationJpaEntity c WHERE (c.requesterAgentId = :agentId OR c.inviteeAgentId = :agentId) AND c.state IN :states")
    long countByAgentIdAndStates(@org.springframework.data.repository.query.Param("agentId") UUID agentId, @org.springframework.data.repository.query.Param("states") List<String> states);

    default List<CoordinationJpaEntity> findTopNByAgentId(UUID agentId, int n) {
        return findByRequesterAgentIdOrInviteeAgentIdOrderByCreatedAtDesc(
                agentId, agentId, PageRequest.of(0, n)).getContent();
    }

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = 
            "SELECT CAST(created_at AS date) as day, " +
            "SUM(CASE WHEN state = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "SUM(CASE WHEN state = 'REJECTED' THEN 1 ELSE 0 END) as rejected, " +
            "SUM(CASE WHEN state = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM coordinations " +
            "WHERE (requester_agent_id = :agentId OR invitee_agent_id = :agentId) " +
            "AND created_at >= :since " +
            "GROUP BY day " +
            "ORDER BY day ASC")
    List<ActivityStatsProjection> findActivityStats(@org.springframework.data.repository.query.Param("agentId") UUID agentId, @org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
