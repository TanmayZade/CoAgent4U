package com.coagent4u.shared;

import java.util.Objects;

/** Slack platform user identifier (e.g. "U01ABC123"). */
public record SlackUserId(String value) {
    public SlackUserId {
        Objects.requireNonNull(value, "SlackUserId value must not be null");
        if (value.isBlank())
            throw new IllegalArgumentException("SlackUserId value must not be blank");
    }

    public static SlackUserId of(String value) {
        return new SlackUserId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
