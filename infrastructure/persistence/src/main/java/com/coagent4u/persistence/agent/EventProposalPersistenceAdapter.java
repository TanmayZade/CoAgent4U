package com.coagent4u.persistence.agent;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.coagent4u.agent.domain.EventProposal;
import com.coagent4u.agent.domain.EventProposalStatus;
import com.coagent4u.agent.port.out.EventProposalPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.EventProposalId;

/**
 * Persistence adapter translating between EventProposal domain entity and JPA.
 */
@Repository
public class EventProposalPersistenceAdapter implements EventProposalPersistencePort {

    private final EventProposalJpaRepository repository;

    public EventProposalPersistenceAdapter(EventProposalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EventProposal proposal) {
        EventProposalJpaEntity entity = toJpa(proposal);
        repository.save(entity);
    }

    @Override
    public Optional<EventProposal> findById(EventProposalId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<EventProposal> findByApprovalId(ApprovalId approvalId) {
        return repository.findByApprovalId(approvalId.value()).map(this::toDomain);
    }

    // ── Mapping ──

    private EventProposalJpaEntity toJpa(EventProposal p) {
        return new EventProposalJpaEntity(
                p.getProposalId().value(),
                p.getAgentId().value(),
                p.getApprovalId() != null ? p.getApprovalId().value() : null,
                p.getTitle(),
                p.getStartTime(),
                p.getEndTime(),
                p.getStatus().name(),
                p.getEventId() != null ? p.getEventId().value() : null,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private EventProposal toDomain(EventProposalJpaEntity e) {
        return new EventProposal(
                new EventProposalId(e.getId()),
                new AgentId(e.getAgentId()),
                e.getTitle(),
                e.getStartTime(),
                e.getEndTime(),
                EventProposalStatus.valueOf(e.getStatus()),
                e.getApprovalId() != null ? new ApprovalId(e.getApprovalId()) : null,
                e.getEventId() != null ? new EventId(e.getEventId()) : null,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
