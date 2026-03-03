package com.coagent4u.agent.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IntentParserTest {

    private final IntentParser parser = new IntentParser();

    // ── ADD_EVENT ──
    @ParameterizedTest
    @ValueSource(strings = {
            "add event standup on Monday",
            "schedule a meeting called 'team sync' for tomorrow",
            "create event review at 3pm",
            "book a call called onboarding on Friday"
    })
    void addEvent_patterns(String input) {
        ParsedIntent intent = parser.parse(input);
        assertEquals(IntentType.ADD_EVENT, intent.type());
    }

    // ── CANCEL_EVENT ──
    @ParameterizedTest
    @ValueSource(strings = {
            "cancel event standup",
            "delete the meeting called team sync",
            "remove event onboarding"
    })
    void cancelEvent_patterns(String input) {
        ParsedIntent intent = parser.parse(input);
        assertEquals(IntentType.CANCEL_EVENT, intent.type());
    }

    // ── VIEW_SCHEDULE ──
    @ParameterizedTest
    @ValueSource(strings = {
            "show my schedule",
            "view my calendar",
            "what's on my agenda",
            "check my events"
    })
    void viewSchedule_patterns(String input) {
        ParsedIntent intent = parser.parse(input);
        assertEquals(IntentType.VIEW_SCHEDULE, intent.type());
    }

    // ── SCHEDULE_WITH ──
    @ParameterizedTest
    @ValueSource(strings = {
            "schedule a meeting with @alice",
            "coordinate with bob",
            "set up a call with charlie",
            "arrange a session with @dave"
    })
    void scheduleWith_patterns(String input) {
        ParsedIntent intent = parser.parse(input);
        assertEquals(IntentType.SCHEDULE_WITH, intent.type());
        assertFalse(intent.param("targetUser").isEmpty());
    }

    // ── UNKNOWN ──
    @Test
    void unknown_randomText() {
        ParsedIntent intent = parser.parse("hello world");
        assertEquals(IntentType.UNKNOWN, intent.type());
    }

    @Test
    void unknown_null() {
        ParsedIntent intent = parser.parse(null);
        assertEquals(IntentType.UNKNOWN, intent.type());
    }

    @Test
    void unknown_blank() {
        ParsedIntent intent = parser.parse("   ");
        assertEquals(IntentType.UNKNOWN, intent.type());
    }

    // ── Priority: SCHEDULE_WITH over ADD_EVENT ──
    @Test
    void scheduleWith_takePriority() {
        // "schedule a meeting with @alice" could match ADD_EVENT too
        ParsedIntent intent = parser.parse("schedule a meeting with @alice");
        assertEquals(IntentType.SCHEDULE_WITH, intent.type());
    }

    // ── Title extraction ──
    @Test
    void addEvent_extractsTitle() {
        ParsedIntent intent = parser.parse("add event standup on Monday");
        assertEquals("standup", intent.param("title"));
    }
}
