package com.coagent4u.agent.capability;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.approval.port.in.CreateApprovalUseCase;
import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.Duration;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Implements {@link AgentApprovalPort} (coordination-module's outbound
 * interface)
 * by delegating to the approval-module's {@link CreateApprovalUseCase}.
 *
 * Also sends the Slack approval notification card to the agent's user.
 */
public class AgentApprovalPortImpl implements AgentApprovalPort {

        private static final Logger log = LoggerFactory.getLogger(AgentApprovalPortImpl.class);
        private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy",
                        Locale.ENGLISH);
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

        private final AgentPersistencePort agentPersistence;
        private final UserPersistencePort userPersistence;
        private final CreateApprovalUseCase createApprovalUseCase;
        private final NotificationPort notificationPort;

        public AgentApprovalPortImpl(AgentPersistencePort agentPersistence,
                        UserPersistencePort userPersistence,
                        CreateApprovalUseCase createApprovalUseCase,
                        NotificationPort notificationPort) {
                this.agentPersistence = agentPersistence;
                this.userPersistence = userPersistence;
                this.createApprovalUseCase = createApprovalUseCase;
                this.notificationPort = notificationPort;
        }

        @Override
        public ApprovalId requestApproval(AgentId agentId, MeetingProposal proposal) {
                var agent = agentPersistence.findById(agentId)
                                .orElseThrow(() -> new java.util.NoSuchElementException("Agent not found: " + agentId));

                // Extract coordinationId from proposal (proposalId is the coordination UUID
                // Extract coordinationId from proposal's coordinationIdStr field
                CoordinationId coordinationId = new CoordinationId(
                                java.util.UUID.fromString(proposal.coordinationIdStr()));

                ApprovalId approvalId = createApprovalUseCase.create(
                                agent.getUserId(),
                                ApprovalType.COLLABORATIVE,
                                coordinationId,
                                Duration.ofHours(12) // 12-hour timeout per PRD
                );

                // Send Slack approval card to the user
                sendApprovalCard(agent.getUserId(), agentId, proposal, approvalId);

                return approvalId;
        }

        /**
         * Sends a Slack approval card with meeting details and the counterparty's
         * mention.
         */
        private void sendApprovalCard(com.coagent4u.shared.UserId userId, AgentId thisAgentId,
                        MeetingProposal proposal, ApprovalId approvalId) {
                try {
                        User user = userPersistence.findById(userId).orElse(null);
                        if (user == null) {
                                log.warn("[AgentApprovalPort] No user found for userId={}", userId);
                                return;
                        }

                        // Determine who the "other party" is (requester or invitee)
                        AgentId otherAgentId = thisAgentId.equals(proposal.requesterAgentId())
                                        ? proposal.inviteeAgentId()
                                        : proposal.requesterAgentId();

                        // Resolve the other party's Slack ID for mention
                        String otherPartyMention = resolveSlackMention(otherAgentId);

                        // Format the meeting time
                        Instant start = proposal.suggestedTime().start();
                        Instant end = proposal.suggestedTime().end();
                        String dateStr = start.atZone(IST).format(DATE_FMT);
                        String startTime = start.atZone(IST).format(TIME_FMT);
                        String endTime = end.atZone(IST).format(TIME_FMT);

                        String proposalText = otherPartyMention + " selected a meeting slot.\n\n"
                                        + "📅 " + dateStr + "\n"
                                        + "🕐 " + startTime + " – " + endTime + "\n\n"
                                        + "Approve or reject this meeting time.";

                        notificationPort.sendApprovalRequest(
                                        user.getSlackIdentity().slackUserId(),
                                        user.getSlackIdentity().workspaceId(),
                                        proposalText,
                                        approvalId.value().toString(),
                                        proposal.coordinationIdStr());

                        log.info("[AgentApprovalPort] Approval card sent to user={} for coordination={}",
                                        userId, proposal.proposalId());
                } catch (Exception e) {
                        log.warn("[AgentApprovalPort] Failed to send approval card: {}", e.getMessage());
                }
        }

        /**
         * Resolves an AgentId to a Slack mention string like {@code <@U12345>}.
         */
        private String resolveSlackMention(AgentId agentId) {
                try {
                        var agent = agentPersistence.findById(agentId).orElse(null);
                        if (agent != null) {
                                var user = userPersistence.findById(agent.getUserId()).orElse(null);
                                if (user != null && user.getSlackIdentity() != null) {
                                        return "<@" + user.getSlackIdentity().slackUserId().value() + ">";
                                }
                        }
                } catch (Exception e) {
                        log.warn("[AgentApprovalPort] Failed to resolve Slack mention for agent={}", agentId);
                }
                return "the other participant";
        }
}
