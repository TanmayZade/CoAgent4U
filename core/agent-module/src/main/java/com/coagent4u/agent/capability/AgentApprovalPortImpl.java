package com.coagent4u.agent.capability;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.approval.port.in.CreateApprovalUseCase;
import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.Duration;

/**
 * Implements {@link AgentApprovalPort} (coordination-module's outbound
 * interface)
 * by delegating to the approval-module's {@link CreateApprovalUseCase}.
 */
public class AgentApprovalPortImpl implements AgentApprovalPort {

    private final AgentPersistencePort agentPersistence;
    private final CreateApprovalUseCase createApprovalUseCase;

    public AgentApprovalPortImpl(AgentPersistencePort agentPersistence,
            CreateApprovalUseCase createApprovalUseCase) {
        this.agentPersistence = agentPersistence;
        this.createApprovalUseCase = createApprovalUseCase;
    }

    @Override
    public ApprovalId requestApproval(AgentId agentId, MeetingProposal proposal) {
        var agent = agentPersistence.findById(agentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Agent not found: " + agentId));

        return createApprovalUseCase.create(
                agent.getUserId(),
                ApprovalType.COLLABORATIVE,
                null, // coordinationId will be linked in Phase 2 persistence
                Duration.ofHours(12) // 12-hour timeout per PRD
        );
    }
}
