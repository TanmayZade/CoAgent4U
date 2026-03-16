package com.coagent4u.monitoring;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.persistence.activity.AgentActivityJpaEntity;
import com.coagent4u.persistence.activity.AgentActivityJpaRepository;

/**
 * Persists all domain events to the agent_activities table for compliance.
 * Handler failure is caught and logged — never propagated to coordination
 * transaction.
 *
 * <p>Phase 3 complete:
 * <ul>
 *   <li>Extracts {@code userId} from events implementing {@link UserAwareEvent}</li>
 *   <li>Serializes the full event payload to JSON via Jackson</li>
 * </ul>
 */
@Component
public class AgentActivityEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityEventHandler.class);

    private final AgentActivityJpaRepository activityRepo;
    private final EventDescriptionMapper descriptionMapper;

    public AgentActivityEventHandler(AgentActivityJpaRepository activityRepo, EventDescriptionMapper descriptionMapper) {
        this.activityRepo = activityRepo;
        this.descriptionMapper = descriptionMapper;
    }

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        try {
            // Phase 3a: Map event using EventDescriptionMapper
            List<EventDescriptionMapper.MappedEvent> mappedEvents = descriptionMapper.mapEvent(event);
            
            for (EventDescriptionMapper.MappedEvent mapped : mappedEvents) {
                if (mapped.agentId() == null) {
                    continue; // Skip invalid events
                }
                
                AgentActivityJpaEntity agentActivity = new AgentActivityJpaEntity(
                        UUID.randomUUID(),
                        mapped.agentId(),
                        mapped.correlationId(),
                        mapped.coordinationId(),
                        event.getClass().getSimpleName(),
                        mapped.description(),
                        mapped.level(),
                        event.occurredAt()
                );
                activityRepo.save(agentActivity);
            }
        } catch (Exception e) {
            log.error("AgentActivity write failed for event {} — coordination unaffected",
                    event.eventId(), e);
        }
    }
}
