package com.coagent4u.agent.domain;

/** Type of user intent parsed from a Slack message. */
public enum IntentType {
    ADD_EVENT,
    CANCEL_EVENT,
    VIEW_SCHEDULE,
    SCHEDULE_WITH,
    UNKNOWN
}
