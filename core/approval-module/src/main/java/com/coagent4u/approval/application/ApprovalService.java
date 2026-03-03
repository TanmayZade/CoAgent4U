package com.coagent4u.approval.application;

import java.time.Instant;
import java.util.NoSuchElementException;

import com.coagent4u.approval.domain.Approval;
import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.approval.domain.ExpirationPolicy;
import com.coagent4u.approval.port.in.CreateApprovalUseCase;
import com.coagent4u.approval.port.in.DecideApprovalUseCase;
import com.coagent4u.approval.port.out.ApprovalPersistencePort;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.common.events.ApprovalDecisionMade;
import com.coagent4u.common.events.ApprovalExpired;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.Duration;
import com.coagent4u.shared.UserId;

/**
 * Application service for the Approval bounded context.
 * Implements {@link CreateApprovalUseCase} and {@link DecideApprovalUseCase}.
 *
 * <p>
 * No Spring annotations — assembled by the DI container in coagent-app.
 */
public class ApprovalService implements CreateApprovalUseCase, DecideApprovalUseCase {

    private final ApprovalPersistencePort persistence;
    private final DomainEventPublisher eventPublisher;

    public ApprovalService(ApprovalPersistencePort persistence, DomainEventPublisher eventPublisher) {
        this.persistence = persistence;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ApprovalId create(UserId userId, ApprovalType approvalType,
            CoordinationId coordinationId, Duration timeout) {
        ApprovalId approvalId = ApprovalId.generate();
        Instant expiresAt = ExpirationPolicy.calculateExpiresAt(Instant.now(), timeout.minutes() / 60L);
        Approval approval = new Approval(approvalId, coordinationId, userId, approvalType, expiresAt);
        persistence.save(approval);
        return approvalId;
    }

    @Override
    public void decide(ApprovalId approvalId, UserId userId, ApprovalStatus decision) {
        Approval approval = persistence.findById(approvalId)
                .orElseThrow(() -> new NoSuchElementException("Approval not found: " + approvalId));

        if (!approval.getUserId().equals(userId)) {
            throw new SecurityException("User " + userId + " does not own approval " + approvalId);
        }

        // Auto-expire if past deadline
        if (approval.isExpired(Instant.now())) {
            approval.expire();
            persistence.save(approval);
            eventPublisher.publish(ApprovalExpired.of(
                    approvalId, userId,
                    approval.getApprovalType().name(),
                    approval.getExpiresAt()));
            return;
        }

        approval.decide(decision);
        persistence.save(approval);
        eventPublisher.publish(ApprovalDecisionMade.of(
                approvalId, userId,
                decision.name(),
                approval.getApprovalType().name()));
    }
}
