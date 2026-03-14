package com.coagent4u.app.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public record SentSlotCard(String slackUserId, String workspaceId, String coordinationId,
            List<TimeSlot> slots, String requesterMention) {
    }

    private final List<SentMessage> messages = new ArrayList<>();
    private final List<SentApproval> approvals = new ArrayList<>();
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
    public boolean deleteMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String ts) {
        log.info("Mock delete message for channel={} ts={}", slackUserId.value(), ts);
        // The original mock implementation for deleteMessage simply returned true.
        // The provided snippet introduced 'messagesSent' which is not defined in this class.
        // To maintain syntactic correctness and avoid introducing new state not explicitly requested,
        // we will keep the original mock behavior of returning true, but update the log message
        // and signature as per the instruction.
        // If the intent was to actually remove a message from the 'messages' list,
        // the 'SentMessage' record would need to store the 'ts' (timestamp) returned by sendMessage.
        return true;
    }

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
