package com.coagent4u.persistence.coordination;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.domain.CoordinationStateLogEntry;
import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeSlot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class CoordinationPersistenceAdapter implements CoordinationPersistencePort {

    private static final Logger log = LoggerFactory.getLogger(CoordinationPersistenceAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CoordinationJpaRepository repository;

    public CoordinationPersistenceAdapter(CoordinationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Coordination save(Coordination coordination) {
        CoordinationJpaEntity entity = toJpa(coordination);
        repository.save(entity);
        return coordination;
    }

    @Override
    public Optional<Coordination> findById(CoordinationId coordinationId) {
        return repository.findById(coordinationId.value()).map(this::toDomain);
    }

    @Override
    public void appendStateLog(CoordinationStateLogEntry entry) {
        repository.findById(entry.coordinationId().value()).ifPresent(entity -> {
            StateLogJpaEntity logEntity = new StateLogJpaEntity();
            logEntity.setLogId(entry.logId());
            logEntity.setCoordination(entity); // ManyToOne association
            logEntity.setFromState(entry.fromState() != null ? entry.fromState().name() : "NONE");
            logEntity.setToState(entry.toState().name());
            logEntity.setReason(entry.reason());
            logEntity.setTransitionedAt(entry.timestamp());
            entity.getStateLog().add(logEntity);
            repository.save(entity);
        });
    }

    private CoordinationJpaEntity toJpa(Coordination c) {
        CoordinationJpaEntity entity = new CoordinationJpaEntity();
        entity.setCoordinationId(c.getCoordinationId().value());
        entity.setRequesterAgentId(c.getRequesterAgentId().value());
        entity.setInviteeAgentId(c.getInviteeAgentId().value());
        entity.setState(c.getState().name());
        entity.setProposalJson(
                c.getProposal() != null ? MeetingProposalJsonMapper.toJson(c.getProposal()) : null);
        entity.setCreatedAt(c.getCreatedAt());
        entity.setCompletedAt(c.getCompletedAt());
        entity.setReason(null);

        // Serialize available slots and selected slot
        try {
            if (c.getAvailableSlots() != null && !c.getAvailableSlots().isEmpty()) {
                entity.setAvailableSlotsJson(MAPPER.writeValueAsString(c.getAvailableSlots()));
            }
            if (c.getSelectedSlot() != null) {
                entity.setSelectedSlotJson(MAPPER.writeValueAsString(c.getSelectedSlot()));
            }
        } catch (Exception e) {
            log.warn("[Persistence] Failed to serialize slot data: {}", e.getMessage());
        }

        // State log entries
        List<StateLogJpaEntity> logEntities = new ArrayList<>();
        for (CoordinationStateLogEntry le : c.getStateLog()) {
            StateLogJpaEntity sle = new StateLogJpaEntity();
            sle.setLogId(le.logId());
            sle.setCoordination(entity); // ManyToOne association
            sle.setFromState(le.fromState() != null ? le.fromState().name() : "NONE");
            sle.setToState(le.toState().name());
            sle.setReason(le.reason());
            sle.setTransitionedAt(le.timestamp());
            logEntities.add(sle);
        }
        entity.setStateLog(logEntities);
        return entity;
    }

    /**
     * Reconstitutes Coordination from JPA entity via reflection.
     * Coordination constructor initializes to INITIATED + adds a log entry.
     * Reflection restores actual persisted state without domain contamination.
     */
    private Coordination toDomain(CoordinationJpaEntity e) {
        try {
            CoordinationId coordId = new CoordinationId(e.getCoordinationId());
            AgentId requesterId = new AgentId(e.getRequesterAgentId());
            AgentId inviteeId = new AgentId(e.getInviteeAgentId());

            Coordination coord = new Coordination(coordId, requesterId, inviteeId);

            // Override internal state via reflection
            setField(coord, "state", CoordinationState.valueOf(e.getState()));
            setField(coord, "createdAt", e.getCreatedAt());
            setField(coord, "updatedAt", e.getCreatedAt());
            setField(coord, "completedAt", e.getCompletedAt());

            // Restore proposal from JSONB
            if (e.getProposalJson() != null) {
                MeetingProposal proposal = MeetingProposalJsonMapper.fromJson(e.getProposalJson());
                coord.setProposal(proposal);
            }

            // Restore available slots from JSONB
            if (e.getAvailableSlotsJson() != null && !e.getAvailableSlotsJson().isBlank()) {
                List<TimeSlot> slots = MAPPER.readValue(e.getAvailableSlotsJson(),
                        new TypeReference<List<TimeSlot>>() {
                        });
                coord.setAvailableSlots(slots);
            }

            // Restore selected slot from JSONB
            if (e.getSelectedSlotJson() != null && !e.getSelectedSlotJson().isBlank()) {
                TimeSlot selectedSlot = MAPPER.readValue(e.getSelectedSlotJson(), TimeSlot.class);
                setField(coord, "selectedSlot", selectedSlot);
            }

            // Clear constructor-created stateLog and restore from DB
            List<CoordinationStateLogEntry> restoredLog = new ArrayList<>();
            for (StateLogJpaEntity le : e.getStateLog()) {
                restoredLog.add(new CoordinationStateLogEntry(
                        le.getLogId(),
                        coordId,
                        "NONE".equals(le.getFromState()) ? null : CoordinationState.valueOf(le.getFromState()),
                        CoordinationState.valueOf(le.getToState()),
                        le.getReason(),
                        le.getTransitionedAt()));
            }
            setField(coord, "stateLog", restoredLog);

            return coord;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to reconstitute Coordination from JPA entity", ex);
        }
    }

    private static void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
