package com.coagent4u.approval.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

class ApprovalTest {

    private Approval createPending() {
        return new Approval(ApprovalId.generate(), CoordinationId.generate(),
                UserId.generate(), ApprovalType.COLLABORATIVE,
                Instant.now().plusSeconds(3600));
    }

    @Test
    void newApproval_isPending() {
        Approval a = createPending();
        assertTrue(a.isPending());
        assertEquals(ApprovalStatus.PENDING, a.getStatus());
        assertNull(a.getDecidedAt());
    }

    @Test
    void decide_approved() {
        Approval a = createPending();
        a.decide(ApprovalStatus.APPROVED);
        assertEquals(ApprovalStatus.APPROVED, a.getStatus());
        assertFalse(a.isPending());
        assertNotNull(a.getDecidedAt());
    }

    @Test
    void decide_rejected() {
        Approval a = createPending();
        a.decide(ApprovalStatus.REJECTED);
        assertEquals(ApprovalStatus.REJECTED, a.getStatus());
    }

    @Test
    void decide_pending_rejects() {
        Approval a = createPending();
        assertThrows(IllegalArgumentException.class, () -> a.decide(ApprovalStatus.PENDING));
    }

    @Test
    void decide_expired_rejects() {
        Approval a = createPending();
        assertThrows(IllegalArgumentException.class, () -> a.decide(ApprovalStatus.EXPIRED));
    }

    @Test
    void decide_alreadyDecided_rejects() {
        Approval a = createPending();
        a.decide(ApprovalStatus.APPROVED);
        assertThrows(IllegalStateException.class, () -> a.decide(ApprovalStatus.REJECTED));
    }

    @Test
    void expire_pendingApproval() {
        Approval a = createPending();
        a.expire();
        assertEquals(ApprovalStatus.EXPIRED, a.getStatus());
        assertNotNull(a.getDecidedAt());
    }

    @Test
    void expire_alreadyDecided_rejects() {
        Approval a = createPending();
        a.decide(ApprovalStatus.APPROVED);
        assertThrows(IllegalStateException.class, a::expire);
    }

    @Test
    void isExpired_pastDeadline() {
        Approval a = new Approval(ApprovalId.generate(), null,
                UserId.generate(), ApprovalType.PERSONAL,
                Instant.now().minusSeconds(60)); // already expired
        assertTrue(a.isExpired(Instant.now()));
    }

    @Test
    void isExpired_beforeDeadline() {
        Approval a = new Approval(ApprovalId.generate(), null,
                UserId.generate(), ApprovalType.PERSONAL,
                Instant.now().plusSeconds(3600));
        assertFalse(a.isExpired(Instant.now()));
    }

    @Test
    void personalApproval_nullCoordinationId() {
        Approval a = new Approval(ApprovalId.generate(), null,
                UserId.generate(), ApprovalType.PERSONAL,
                Instant.now().plusSeconds(3600));
        assertNull(a.getCoordinationId());
        assertEquals(ApprovalType.PERSONAL, a.getApprovalType());
    }
}
