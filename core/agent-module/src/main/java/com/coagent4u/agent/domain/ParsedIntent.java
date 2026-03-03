package com.coagent4u.agent.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Parsed representation of a user's Slack message intent. */
public record ParsedIntent(
        IntentType type,
        String rawText,
        Map<String, String> params // extracted named parameters e.g. "title", "targetUser"
) {
    public ParsedIntent {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        params = params == null ? Collections.emptyMap() : Collections.unmodifiableMap(params);
    }

    public String param(String key) {
        return params.getOrDefault(key, "");
    }
}
