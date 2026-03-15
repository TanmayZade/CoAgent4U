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

    default List<CoordinationJpaEntity> findTopNByAgentId(UUID agentId, int n) {
        return findByRequesterAgentIdOrInviteeAgentIdOrderByCreatedAtDesc(
                agentId, agentId, PageRequest.of(0, n)).getContent();
    }
}
