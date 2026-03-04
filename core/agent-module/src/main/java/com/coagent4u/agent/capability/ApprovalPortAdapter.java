package com.coagent4u.agent.capability;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.ApprovalPort;
import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.approval.port.in.CreateApprovalUseCase;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.Duration;

/**
 * Implements {@link ApprovalPort} (agent-module's outbound interface)
 * by delegating to the approval-module's {@link CreateApprovalUseCase}.
 *
 * <p>
 * No Spring annotations — wired via BeanWiringConfig in coagent-app.
 * </p>
 */
public class ApprovalPortAdapter implements ApprovalPort {

    private final AgentPersistencePort agentPersistence;
    private final CreateApprovalUseCase createApprovalUseCase;

    public ApprovalPortAdapter(AgentPersistencePort agentPersistence,
            CreateApprovalUseCase createApprovalUseCase) {
        this.agentPersistence = agentPersistence;
        this.createApprovalUseCase = createApprovalUseCase;
    }

    @Override
    public ApprovalId requestPersonalApproval(AgentId agentId, String context, Duration timeout) {
        var agent = agentPersistence.findById(agentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Agent not found: " + agentId));

        return createApprovalUseCase.create(
                agent.getUserId(),
                ApprovalType.PERSONAL,
                null, // no coordination for personal approvals
                timeout);
    }
}
