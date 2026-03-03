package com.coagent4u.agent.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service — Tier 1 rule-based intent parser.
 *
 * <p>
 * Parses natural-language Slack messages to identify user intent.
 * Uses regex pattern matching before delegating to LLM (Tier 2) for ambiguous
 * cases.
 * This class is pure Java with zero external dependencies.
 */
public class IntentParser {

    // Pattern: "add/schedule/create event <title> on/at ..."
    private static final Pattern ADD_EVENT = Pattern.compile(
            "(?i)\\b(add|create|schedule|book|set up)\\s+(a\\s+)?(meeting|event|appointment|call|session)?\\s*(?:called|named|titled)?\\s*[\"']?([^\"']+?)[\"']?\\s*(?:on|at|for)\\b",
            Pattern.CASE_INSENSITIVE);

    // Pattern: "cancel/delete/remove event <title>"
    private static final Pattern CANCEL_EVENT = Pattern.compile(
            "(?i)\\b(cancel|delete|remove|clear)\\s+(the\\s+)?(meeting|event|appointment|call)?\\s*(?:called|named|titled)?\\s*[\"']?([^\"']+?)[\"']?(?:\\s+(?:on|at|for|tomorrow|today))?\\b",
            Pattern.CASE_INSENSITIVE);

    // Pattern: "show/view/what's on my schedule/calendar"
    private static final Pattern VIEW_SCHEDULE = Pattern.compile(
            "(?i)\\b(show|view|display|what'?s?|check)\\s+(on\\s+)?(my\\s+)?(schedule|calendar|agenda|events?|appointments?)\\b",
            Pattern.CASE_INSENSITIVE);

    // Pattern: "schedule a meeting with @user / coordinate with ..."
    private static final Pattern SCHEDULE_WITH = Pattern.compile(
            "(?i)\\b(schedule|arrange|set up|coordinate|book)\\s+(a\\s+)?(meeting|call|session|event)?\\s*(with|alongside)\\s+(@?[\\w.]+)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parses a raw Slack message text to a {@link ParsedIntent}.
     *
     * @param rawText the message text
     * @return the parsed intent (type UNKNOWN if no pattern matches)
     */
    public ParsedIntent parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedIntent(IntentType.UNKNOWN, rawText != null ? rawText : "", Map.of());
        }

        Matcher m;

        m = SCHEDULE_WITH.matcher(rawText);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            if (m.group(5) != null)
                params.put("targetUser", m.group(5).replace("@", ""));
            return new ParsedIntent(IntentType.SCHEDULE_WITH, rawText, params);
        }

        m = ADD_EVENT.matcher(rawText);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            if (m.group(4) != null)
                params.put("title", m.group(4).trim());
            return new ParsedIntent(IntentType.ADD_EVENT, rawText, params);
        }

        m = CANCEL_EVENT.matcher(rawText);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            if (m.group(4) != null)
                params.put("title", m.group(4).trim());
            return new ParsedIntent(IntentType.CANCEL_EVENT, rawText, params);
        }

        m = VIEW_SCHEDULE.matcher(rawText);
        if (m.find()) {
            return new ParsedIntent(IntentType.VIEW_SCHEDULE, rawText, Map.of());
        }

        return new ParsedIntent(IntentType.UNKNOWN, rawText, Map.of());
    }
}
