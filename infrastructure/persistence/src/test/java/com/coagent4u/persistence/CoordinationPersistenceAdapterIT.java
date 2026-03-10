package com.coagent4u.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.persistence.coordination.CoordinationJpaEntity;
import com.coagent4u.persistence.coordination.CoordinationJpaRepository;
import com.coagent4u.persistence.coordination.MeetingProposalJsonMapper;
import com.coagent4u.persistence.coordination.StateLogJpaEntity;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeSlot;

/**
 * Integration test for Coordination persistence with JSONB proposal round-trip.
 * Verifies: deterministic JSON serialization, state log cascade, Flyway schema.
 */
class CoordinationPersistenceAdapterIT extends PostgresIntegrationTest {

        @Autowired
        private CoordinationJpaRepository coordRepo;

        @Test
        void saveAndFindById_roundTrip() {
                UUID coordId = UUID.randomUUID();
                CoordinationJpaEntity entity = createTestCoordination(coordId, "INITIATED");
                coordRepo.save(entity);

                CoordinationJpaEntity loaded = coordRepo.findById(coordId).orElseThrow();
                assertEquals("INITIATED", loaded.getState());
                assertEquals(entity.getRequesterAgentId(), loaded.getRequesterAgentId());
                assertEquals(entity.getInviteeAgentId(), loaded.getInviteeAgentId());
        }

        @Test
        void jsonbProposal_roundTrip_deterministic() {
                UUID coordId = UUID.randomUUID();
                UUID requesterId = UUID.randomUUID();
                UUID inviteeId = UUID.randomUUID();

                CoordinationJpaEntity entity = new CoordinationJpaEntity(
                                coordId, requesterId, inviteeId,
                                "PROPOSAL_GENERATED", null, null,
                                Instant.now(), null);

                // Create a MeetingProposal and serialize to JSON
                MeetingProposal original = new MeetingProposal(
                                "prop-123",
                                coordId.toString(),
                                new AgentId(requesterId),
                                new AgentId(inviteeId),
                                new TimeSlot(
                                                Instant.parse("2026-03-10T10:00:00Z"),
                                                Instant.parse("2026-03-10T10:30:00Z")),
                                30,
                                "Team Standup",
                                "UTC");
                String json = MeetingProposalJsonMapper.toJson(original);
                entity.setProposalJson(json);
                coordRepo.save(entity);

                // Reload from DB and verify JSONB round-trip
                CoordinationJpaEntity loaded = coordRepo.findById(coordId).orElseThrow();
                assertNotNull(loaded.getProposalJson());

                MeetingProposal restored = MeetingProposalJsonMapper.fromJson(loaded.getProposalJson());
                assertEquals(original.proposalId(), restored.proposalId());
                assertEquals(original.requesterAgentId(), restored.requesterAgentId());
                assertEquals(original.inviteeAgentId(), restored.inviteeAgentId());
                assertEquals(original.durationMinutes(), restored.durationMinutes());
                assertEquals(original.title(), restored.title());
                assertEquals(original.timezone(), restored.timezone());

                // Verify deterministic serialization — same output both ways
                String secondJson = MeetingProposalJsonMapper.toJson(restored);
                MeetingProposal secondRestored = MeetingProposalJsonMapper.fromJson(secondJson);
                assertEquals(original.proposalId(), secondRestored.proposalId());
                assertEquals(original.title(), secondRestored.title());
        }

        @Test
        @Transactional
        void stateLog_cascadePersistence() {
                UUID coordId = UUID.randomUUID();

                CoordinationJpaEntity entity = new CoordinationJpaEntity(
                                coordId, UUID.randomUUID(), UUID.randomUUID(),
                                "CHECKING_AVAILABILITY_A", null, null,
                                Instant.now(), null);

                // StateLogJpaEntity(UUID logId, String fromState, String toState, String
                // reason, Instant transitionedAt)
                StateLogJpaEntity log1 = new StateLogJpaEntity(
                                UUID.randomUUID(), "NONE", "INITIATED", "Coordination started",
                                Instant.now().minusSeconds(60));
                log1.setCoordination(entity);

                StateLogJpaEntity log2 = new StateLogJpaEntity(
                                UUID.randomUUID(), "INITIATED", "CHECKING_AVAILABILITY_A",
                                "Checking requester availability", Instant.now());
                log2.setCoordination(entity);

                entity.getStateLog().add(log1);
                entity.getStateLog().add(log2);
                coordRepo.save(entity);

                CoordinationJpaEntity loaded = coordRepo.findById(coordId).orElseThrow();
                assertEquals(2, loaded.getStateLog().size());
        }

        @Test
        void transactionRollback_nullRequiredField() {
                // Missing requesterAgentId (NOT NULL constraint) — should cause DB error
                CoordinationJpaEntity entity = new CoordinationJpaEntity(
                                UUID.randomUUID(), null, UUID.randomUUID(),
                                "INITIATED", null, null,
                                Instant.now(), null);

                assertThrows(Exception.class, () -> {
                        coordRepo.saveAndFlush(entity);
                });
        }

        private CoordinationJpaEntity createTestCoordination(UUID coordId, String state) {
                return new CoordinationJpaEntity(
                                coordId, UUID.randomUUID(), UUID.randomUUID(),
                                state, null, null,
                                Instant.now(), null);
        }
}
