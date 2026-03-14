package com.coagent4u.coordination.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;

/**
 * In-memory mock of {@link NotificationPort} that captures sent messages
 * for assertion in tests. Supports configurable failures.
 */
public class MockNotificationAdapter implements NotificationPort {

    public record SentMessage(String slackUserId, String workspaceId, String text) {
    }

    public record SentApproval(String slackUserId, String workspaceId, String proposalText, String approvalId) {
    }

    public record SentSlotCard(String slackUserId, String workspaceId, String coordinationId,
            List<TimeSlot> slots, String requesterMention) {
    }

    private final List<SentMessage> messages = new ArrayList<>();
    private final List<SentApproval> approvals = new ArrayList<>();
    private final List<SentSlotCard> slotCards = new ArrayList<>();
    private boolean shouldFail = false;

    /** Configure this mock to throw on next call. */
    public void setShouldFail(boolean fail) {
        this.shouldFail = fail;
    }

    @Override
    public String sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String text) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        messages.add(new SentMessage(slackUserId.value(), workspaceId.value(), text));
        return "mock_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
            String proposalText, String approvalId, String coordinationId) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        approvals.add(new SentApproval(slackUserId.value(), workspaceId.value(), proposalText, approvalId));
        return "mock_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
            String coordinationId, List<TimeSlot> slots, String requesterMention) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        slotCards.add(new SentSlotCard(slackUserId.value(), workspaceId.value(),
                coordinationId, slots, requesterMention));
        return "mock_ts_" + System.currentTimeMillis();
    }

    @Override
    public boolean deleteMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String ts) {
        return true;
    }

    // ── Getters for assertions ──

    public List<SentMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<SentApproval> getApprovals() {
        return Collections.unmodifiableList(approvals);
    }

    public List<SentSlotCard> getSlotCards() {
        return Collections.unmodifiableList(slotCards);
    }

    public void reset() {
        messages.clear();
        approvals.clear();
        slotCards.clear();
        shouldFail = false;
    }
}
