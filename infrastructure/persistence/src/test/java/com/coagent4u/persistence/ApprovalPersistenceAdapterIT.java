package com.coagent4u.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coagent4u.persistence.approval.ApprovalJpaEntity;
import com.coagent4u.persistence.approval.ApprovalJpaRepository;

/**
 * Integration test for Approval persistence using real PostgreSQL.
 * Verifies: save/find round-trip, findPendingByUser query, decision update.
 */
class ApprovalPersistenceAdapterIT extends PostgresIntegrationTest {

    @Autowired
    private ApprovalJpaRepository approvalRepo;

    @Test
    void saveAndFindById_roundTrip() {
        UUID approvalId = UUID.randomUUID();
        ApprovalJpaEntity entity = createTestApproval(approvalId, "PENDING");
        approvalRepo.save(entity);

        ApprovalJpaEntity loaded = approvalRepo.findById(approvalId).orElseThrow();
        assertEquals("PENDING", loaded.getDecision());
        assertEquals("COLLABORATIVE", loaded.getApprovalType());
    }

    @Test
    void findPendingByUser() {
        UUID userId = UUID.randomUUID();

        // Create 2 PENDING and 1 APPROVED approval for same user
        approvalRepo.save(createTestApprovalForUser(userId, "PENDING"));
        approvalRepo.save(createTestApprovalForUser(userId, "PENDING"));
        approvalRepo.save(createTestApprovalForUser(userId, "APPROVED"));

        List<ApprovalJpaEntity> pending = approvalRepo.findByUserIdAndDecision(userId, "PENDING");
        assertEquals(2, pending.size());
    }

    @Test
    void decisionUpdate_persists() {
        UUID approvalId = UUID.randomUUID();
        ApprovalJpaEntity entity = createTestApproval(approvalId, "PENDING");
        approvalRepo.save(entity);

        // Update decision
        ApprovalJpaEntity loaded = approvalRepo.findById(approvalId).orElseThrow();
        loaded.setDecision("APPROVED");
        loaded.setDecidedAt(Instant.now());
        approvalRepo.save(loaded);

        ApprovalJpaEntity reloaded = approvalRepo.findById(approvalId).orElseThrow();
        assertEquals("APPROVED", reloaded.getDecision());
        assertNotNull(reloaded.getDecidedAt());
    }

    private ApprovalJpaEntity createTestApproval(UUID approvalId, String decision) {
        return new ApprovalJpaEntity(
                approvalId,
                UUID.randomUUID(), // coordinationId
                UUID.randomUUID(), // userId
                "COLLABORATIVE",
                decision,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now());
    }

    private ApprovalJpaEntity createTestApprovalForUser(UUID userId, String decision) {
        return new ApprovalJpaEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                "COLLABORATIVE",
                decision,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now());
    }
}
