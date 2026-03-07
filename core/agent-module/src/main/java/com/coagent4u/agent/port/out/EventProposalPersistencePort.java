package com.coagent4u.agent.port.out;

import java.util.Optional;

import com.coagent4u.agent.domain.EventProposal;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.EventProposalId;

/**
 * Outbound port for persisting event proposals.
 */
public interface EventProposalPersistencePort {

    void save(EventProposal proposal);

    Optional<EventProposal> findById(EventProposalId id);

    Optional<EventProposal> findByApprovalId(ApprovalId approvalId);
}
