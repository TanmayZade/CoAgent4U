package com.coagent4u.agent.application;

import java.util.List;
import java.util.NoSuchElementException;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.domain.ConflictDetector;
import com.coagent4u.agent.domain.IntentParser;
import com.coagent4u.agent.domain.IntentType;
import com.coagent4u.agent.domain.ParsedIntent;
import com.coagent4u.agent.port.in.CreatePersonalEventUseCase;
import com.coagent4u.agent.port.in.HandleMessageUseCase;
import com.coagent4u.agent.port.in.ViewScheduleUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.ApprovalPort;
import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.common.events.PersonalEventCreated;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.Duration;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.user.port.out.NotificationPort;

/**
 * Application service for the Agent bounded context.
 * Implements all three inbound use case ports.
 *
 * <p>
 * Orchestration flow for a Slack message:
 * 1. Parse intent (Tier 1 regex → Tier 2 LLM fallback)
 * 2. Route to the correct handler
 * 3. Execute domain logic (conflict check, approval, coordination)
 * 4. Publish domain events
 *
 * <p>
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class AgentCommandService
        implements HandleMessageUseCase, ViewScheduleUseCase, CreatePersonalEventUseCase {

    private final AgentPersistencePort agentPersistence;
    private final CalendarPort calendarPort;
    private final LLMPort llmPort;
    private final ApprovalPort approvalPort;
    private final CoordinationProtocolPort coordinationProtocol;
    private final NotificationPort notificationPort;
    private final DomainEventPublisher eventPublisher;

    private final IntentParser intentParser = new IntentParser();
    private final ConflictDetector conflictDetector = new ConflictDetector();

    public AgentCommandService(AgentPersistencePort agentPersistence,
            CalendarPort calendarPort,
            LLMPort llmPort,
            ApprovalPort approvalPort,
            CoordinationProtocolPort coordinationProtocol,
            NotificationPort notificationPort,
            DomainEventPublisher eventPublisher) {
        this.agentPersistence = agentPersistence;
        this.calendarPort = calendarPort;
        this.llmPort = llmPort;
        this.approvalPort = approvalPort;
        this.coordinationProtocol = coordinationProtocol;
        this.notificationPort = notificationPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void handleMessage(AgentId agentId, String rawText) {
        Agent agent = loadAgent(agentId);

        // Tier 1: rule-based parsing
        ParsedIntent intent = intentParser.parse(rawText);

        // Tier 2: LLM fallback for UNKNOWN
        if (intent.type() == IntentType.UNKNOWN) {
            llmPort.classifyIntent(agentId, rawText)
                    .map(type -> new ParsedIntent(IntentType.valueOf(type), rawText, intent.params()))
                    .ifPresent(llmIntent -> routeIntent(agent, llmIntent));
            return;
        }

        routeIntent(agent, intent);
    }

    private void routeIntent(Agent agent, ParsedIntent intent) {
        switch (intent.type()) {
            case VIEW_SCHEDULE -> notifySchedule(agent);
            case ADD_EVENT -> requestPersonalEventApproval(agent, intent);
            case SCHEDULE_WITH -> initiateCollaboration(agent, intent);
            default -> {
                // CANCEL_EVENT, UNKNOWN — Phase 3 will add full implementation
            }
        }
    }

    @Override
    public List<TimeSlot> viewSchedule(AgentId agentId, TimeRange range) {
        return calendarPort.getEvents(agentId, range);
    }

    @Override
    public EventId createPersonalEvent(AgentId agentId, String title, TimeSlot timeSlot) {
        Agent agent = loadAgent(agentId);
        // Conflict detection
        TimeRange range = TimeRange.of(
                java.time.LocalDate.ofInstant(timeSlot.start(), java.time.ZoneOffset.UTC),
                java.time.LocalDate.ofInstant(timeSlot.end(), java.time.ZoneOffset.UTC));
        List<TimeSlot> existing = calendarPort.getEvents(agentId, range);

        if (conflictDetector.hasConflict(existing, timeSlot)) {
            throw new IllegalStateException("Time slot conflicts with existing event for agent " + agentId);
        }

        EventId eventId = calendarPort.createEvent(agentId, timeSlot, title);
        eventPublisher.publish(PersonalEventCreated.of(agentId, agent.getUserId(), eventId, timeSlot, title));
        return eventId;
    }

    private void notifySchedule(Agent agent) {
        // Phase 3: format and send Slack message
    }

    private void requestPersonalEventApproval(Agent agent, ParsedIntent intent) {
        String title = intent.param("title");
        approvalPort.requestPersonalApproval(agent.getAgentId(), "Create event: " + title,
                Duration.ofHours(12));
    }

    private void initiateCollaboration(Agent agent, ParsedIntent intent) {
        // Phase 3: resolve targetUser → agentId and call
        // coordinationProtocol.initiate()
    }

    private Agent loadAgent(AgentId agentId) {
        return agentPersistence.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + agentId));
    }
}
