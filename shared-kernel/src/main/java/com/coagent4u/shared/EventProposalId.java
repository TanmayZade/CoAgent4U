package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an event proposal.
 */
public record EventProposalId(UUID value) {

    public EventProposalId {
        Objects.requireNonNull(value, "EventProposalId value must not be null");
    }

    public static EventProposalId generate() {
        return new EventProposalId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
