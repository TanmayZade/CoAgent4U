package com.coagent4u.agent.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.EventProposalId;

/**
 * Domain entity representing a personal event proposal.
 * Drives the state machine from INITIATED through approval to calendar
 * creation.
 *
 * <p>
 * All state transitions are validated — illegal transitions throw
 * {@link IllegalStateException}.
 * </p>
 */
public class EventProposal {

    private final EventProposalId proposalId;
    private final AgentId agentId;
    private final String title;
    private final Instant startTime;
    private final Instant endTime;
    private final Instant createdAt;

    private ApprovalId approvalId;
    private EventProposalStatus status;
    private EventId eventId;
    private Instant updatedAt;

    /**
     * Creates a new proposal in INITIATED state.
     */
    public EventProposal(EventProposalId proposalId, AgentId agentId,
            String title, Instant startTime, Instant endTime) {
        this.proposalId = Objects.requireNonNull(proposalId);
        this.agentId = Objects.requireNonNull(agentId);
        this.title = Objects.requireNonNull(title);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.status = EventProposalStatus.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Reconstruction constructor (from persistence).
     */
    public EventProposal(EventProposalId proposalId, AgentId agentId,
            String title, Instant startTime, Instant endTime,
            EventProposalStatus status, ApprovalId approvalId,
            EventId eventId, Instant createdAt, Instant updatedAt) {
        this.proposalId = proposalId;
        this.agentId = agentId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.approvalId = approvalId;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ── State transitions ──

    /**
     * Transitions to a new status with validation.
     */
    public void transitionTo(EventProposalStatus newStatus) {
        this.status.validateTransitionTo(newStatus);
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Links this proposal to an approval request.
     */
    public void linkApproval(ApprovalId approvalId) {
        this.approvalId = Objects.requireNonNull(approvalId);
    }

    /**
     * Records the created Google Calendar event ID.
     */
    public void recordEventId(EventId eventId) {
        this.eventId = Objects.requireNonNull(eventId);
    }

    // ── Getters ──

    public EventProposalId getProposalId() {
        return proposalId;
    }

    public AgentId getAgentId() {
        return agentId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public EventProposalStatus getStatus() {
        return status;
    }

    public ApprovalId getApprovalId() {
        return approvalId;
    }

    public EventId getEventId() {
        return eventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
