package com.coagent4u.persistence.approval;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.coagent4u.approval.domain.Approval;
import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.approval.port.out.ApprovalPersistencePort;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

@Component
public class ApprovalPersistenceAdapter implements ApprovalPersistencePort {

    private final ApprovalJpaRepository repository;

    public ApprovalPersistenceAdapter(ApprovalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Approval save(Approval approval) {
        ApprovalJpaEntity entity = toJpa(approval);
        repository.save(entity);
        return approval;
    }

    @Override
    public Optional<Approval> findById(ApprovalId approvalId) {
        return repository.findById(approvalId.value()).map(this::toDomain);
    }

    @Override
    public List<Approval> findPendingByUser(UserId userId) {
        return repository.findByUserIdAndDecision(userId.value(), "PENDING")
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Approval> findExpiredPending(Instant now) {
        return repository.findByDecisionAndExpiresAtBefore("PENDING", now)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private ApprovalJpaEntity toJpa(Approval a) {
        return new ApprovalJpaEntity(
                a.getApprovalId().value(),
                a.getCoordinationId() != null ? a.getCoordinationId().value() : null,
                a.getUserId().value(),
                a.getApprovalType().name(),
                a.getStatus().name(),
                a.getExpiresAt(),
                a.getDecidedAt(),
                a.getCreatedAt());
    }

    /**
     * Reconstitutes Approval from JPA entity via reflection.
     * Approval has no pullDomainEvents() and its constructor forces PENDING status.
     * Reflection restores the actual persisted state.
     */
    private Approval toDomain(ApprovalJpaEntity e) {
        try {
            Approval approval = new Approval(
                    new ApprovalId(e.getApprovalId()),
                    e.getCoordinationId() != null ? new CoordinationId(e.getCoordinationId()) : null,
                    new UserId(e.getUserId()),
                    ApprovalType.valueOf(e.getApprovalType()),
                    e.getExpiresAt());

            setField(approval, "status", ApprovalStatus.valueOf(e.getDecision()));
            setField(approval, "createdAt", e.getCreatedAt());
            setField(approval, "decidedAt", e.getDecidedAt());

            return approval;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to reconstitute Approval from JPA entity", ex);
        }
    }

    private static void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
