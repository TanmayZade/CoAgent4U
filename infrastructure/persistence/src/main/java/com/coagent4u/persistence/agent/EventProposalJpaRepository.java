package com.coagent4u.persistence.agent;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for event proposals.
 */
public interface EventProposalJpaRepository extends JpaRepository<EventProposalJpaEntity, UUID> {

    Optional<EventProposalJpaEntity> findByApprovalId(UUID approvalId);
}
