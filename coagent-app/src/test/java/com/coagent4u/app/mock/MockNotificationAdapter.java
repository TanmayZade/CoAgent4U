package com.coagent4u.app.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;

/**
 * In-memory mock of {@link NotificationPort} that captures sent messages
 * for assertion in tests. Supports configurable failures.
 */
public class MockNotificationAdapter implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(MockNotificationAdapter.class);

    public record SentMessage(String slackUserId, String workspaceId, String text) {
    }

    public record SentApproval(String slackUserId, String workspaceId, String proposalText, String approvalId) {
    }

    public record SentConflict(String slackUserId, String workspaceId, String proposalText, String approvalId, String existingEventDetails) {
    }

    public record SentSlotCard(String slackUserId, String workspaceId, String coordinationId,
            List<TimeSlot> slots, String requesterMention) {
    }

    private final List<SentMessage> messages = new ArrayList<>();
    private final List<SentApproval> approvals = new ArrayList<>();
    private final List<SentConflict> conflicts = new ArrayList<>();
    private final List<SentSlotCard> slotCards = new ArrayList<>();
    private boolean shouldFail = false;

    public void setShouldFail(boolean fail) {
        this.shouldFail = fail;
    }

    @Override
    public String sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String message) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Message to {}: {}", slackUserId, message);
        messages.add(new SentMessage(slackUserId.value(), workspaceId.value(), message));
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
            String proposalText, String approvalId, String coordinationId) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Approval request to {} for id={} (coord={})",
                slackUserId, approvalId, coordinationId);
        approvals.add(new SentApproval(slackUserId.value(), workspaceId.value(), proposalText, approvalId));
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendConflictResolutionRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
            String proposalText, String approvalId, String existingEventDetails) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Conflict resolution request to {} for id={}",
                slackUserId, approvalId);
        conflicts.add(new SentConflict(slackUserId.value(), workspaceId.value(), proposalText, approvalId, existingEventDetails));
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
            String coordinationId, List<TimeSlot> slots, String requesterMention) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Slot selection for coordination {} sent to {}",
                coordinationId, slackUserId);
        slotCards.add(new SentSlotCard(slackUserId.value(), workspaceId.value(),
                coordinationId, slots, requesterMention));
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendSlotPreview(SlackUserId slackUserId, WorkspaceId workspaceId,
            List<TimeSlot> slots, String inviteeMention) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Slot preview sent to requester {}", slackUserId);
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public String sendStatusCard(SlackUserId slackUserId, WorkspaceId workspaceId, String statusText, String color) {
        if (shouldFail)
            throw new RuntimeException("Mock Slack failure");
        log.info("[MockNotification] Status card to {}: {}", slackUserId, statusText);
        return "dummy_ts_" + System.currentTimeMillis();
    }

    @Override
    public boolean deleteMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String ts) {
        log.info("[MockNotification] Message deleted for {} in workspace {} at ts={}", slackUserId, workspaceId, ts);
        return true;
    }

    public List<SentMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<SentApproval> getApprovals() {
        return Collections.unmodifiableList(approvals);
    }

    public List<SentConflict> getConflicts() {
        return Collections.unmodifiableList(conflicts);
    }

    public List<SentSlotCard> getSlotCards() {
        return Collections.unmodifiableList(slotCards);
    }

    public void reset() {
        messages.clear();
        approvals.clear();
        conflicts.clear();
        slotCards.clear();
        shouldFail = false;
    }
}
