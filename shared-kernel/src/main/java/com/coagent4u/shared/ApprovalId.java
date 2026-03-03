package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for an Approval. */
public record ApprovalId(UUID value) {
    public ApprovalId {
        Objects.requireNonNull(value, "ApprovalId value must not be null");
    }

    public static ApprovalId generate() {
        return new ApprovalId(UUID.randomUUID());
    }

    public static ApprovalId of(UUID value) {
        return new ApprovalId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
