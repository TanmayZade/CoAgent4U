package com.coagent4u.approval.application;

import java.util.List;
import java.util.Optional;

import com.coagent4u.approval.application.dto.PendingApprovalDto;
import com.coagent4u.approval.domain.Approval;
import com.coagent4u.approval.port.in.GetPendingApprovalsUseCase;
import com.coagent4u.approval.port.out.ApprovalPersistencePort;
import com.coagent4u.shared.UserId;

/**
 * Application service for pending approval queries.
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class PendingApprovalsService implements GetPendingApprovalsUseCase {

    /**
     * Functional interface to resolve username → UserId without coupling
     * this module to user-module.
     */
    public interface UserIdResolver {
        Optional<UserId> resolve(String username);
    }

    private final UserIdResolver userIdResolver;
    private final ApprovalPersistencePort approvalPersistence;

    public PendingApprovalsService(UserIdResolver userIdResolver,
                                    ApprovalPersistencePort approvalPersistence) {
        this.userIdResolver = userIdResolver;
        this.approvalPersistence = approvalPersistence;
    }

    @Override
    public List<PendingApprovalDto> getPendingApprovals(String username) {
        UserId userId = userIdResolver.resolve(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

        return approvalPersistence.findPendingByUser(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private PendingApprovalDto toDto(Approval a) {
        return new PendingApprovalDto(
                a.getApprovalId().value(),
                a.getCoordinationId() != null ? a.getCoordinationId().value() : null,
                a.getApprovalType().name(),
                a.getCreatedAt(),
                a.getExpiresAt());
    }
}
