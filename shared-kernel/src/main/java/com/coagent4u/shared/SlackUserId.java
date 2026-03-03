package com.coagent4u.shared;

import java.util.Objects;

/**
 * Strongly-typed wrapper for a Slack user ID (e.g. "U01234ABCDE").
 */
public record SlackUserId(String value) {

    public SlackUserId {
        Objects.requireNonNull(value, "SlackUserId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SlackUserId value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
