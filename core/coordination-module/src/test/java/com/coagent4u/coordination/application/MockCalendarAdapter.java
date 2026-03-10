package com.coagent4u.coordination.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;

/**
 * In-memory mock of {@link AgentEventExecutionPort} that logs event
 * creation/deletion and supports configurable failure for testing
 * the {@link com.coagent4u.coordination.domain.EventCreationSaga}.
 */
public class MockCalendarAdapter implements AgentEventExecutionPort {

    public record CreatedEvent(AgentId agentId, TimeSlot timeSlot, String title, EventId eventId) {
    }

    public record DeletedEvent(AgentId agentId, EventId eventId) {
    }

    private final List<CreatedEvent> createdEvents = new ArrayList<>();
    private final List<DeletedEvent> deletedEvents = new ArrayList<>();
    private boolean failOnCreateA = false;
    private boolean failOnCreateB = false;
    private int createCallCount = 0;

    /** Configure event A creation to throw. */
    public void setFailOnCreateA(boolean fail) {
        this.failOnCreateA = fail;
    }

    /** Configure event B creation to throw. */
    public void setFailOnCreateB(boolean fail) {
        this.failOnCreateB = fail;
    }

    @Override
    public EventId createEvent(AgentId agentId, TimeSlot timeSlot, String title) {
        createCallCount++;
        if (createCallCount == 1 && failOnCreateA) {
            throw new RuntimeException("Mock: Calendar unavailable for event A");
        }
        if (createCallCount == 2 && failOnCreateB) {
            throw new RuntimeException("Mock: Calendar unavailable for event B");
        }
        EventId eventId = new EventId("mock-event-" + createCallCount);
        createdEvents.add(new CreatedEvent(agentId, timeSlot, title, eventId));
        return eventId;
    }

    @Override
    public void deleteEvent(AgentId agentId, EventId eventId) {
        deletedEvents.add(new DeletedEvent(agentId, eventId));
    }

    // ── Getters for assertions ──

    public List<CreatedEvent> getCreatedEvents() {
        return Collections.unmodifiableList(createdEvents);
    }

    public List<DeletedEvent> getDeletedEvents() {
        return Collections.unmodifiableList(deletedEvents);
    }

    public int getCreateCallCount() {
        return createCallCount;
    }

    public void reset() {
        createdEvents.clear();
        deletedEvents.clear();
        createCallCount = 0;
        failOnCreateA = false;
        failOnCreateB = false;
    }
}
