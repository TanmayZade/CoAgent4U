package com.coagent4u.agent.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.domain.ConflictDetector;
import com.coagent4u.agent.domain.EventProposal;
import com.coagent4u.agent.domain.EventProposalStatus;
import com.coagent4u.agent.domain.IntentParser;
import com.coagent4u.agent.domain.IntentType;
import com.coagent4u.agent.domain.NaturalDateResolver;
import com.coagent4u.agent.domain.ParsedIntent;
import com.coagent4u.agent.port.in.CreatePersonalEventUseCase;
import com.coagent4u.agent.port.in.HandleMessageUseCase;
import com.coagent4u.agent.port.in.ViewScheduleUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.ApprovalPort;
import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.agent.port.out.EventProposalPersistencePort;
import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.common.events.PersonalEventCreated;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CalendarEvent;
import com.coagent4u.shared.Duration;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.EventProposalId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Application service for the Agent bounded context.
 * Implements all three inbound use case ports.
 *
 * <p>
 * Orchestration flow for a Slack message:
 * 1. Parse intent (Tier 1 regex → Tier 2 LLM fallback)
 * 2. Route to the correct handler
 * 3. Execute domain logic (conflict check, approval, coordination)
 * 4. Publish domain events
 *
 * <p>
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class AgentCommandService
        implements HandleMessageUseCase, ViewScheduleUseCase, CreatePersonalEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgentCommandService.class);

    private final AgentPersistencePort agentPersistence;
    private final CalendarPort calendarPort;
    private final LLMPort llmPort;
    private final ApprovalPort approvalPort;
    private final CoordinationProtocolPort coordinationProtocol;
    private final NotificationPort notificationPort;
    private final UserPersistencePort userPersistence;
    private final DomainEventPublisher eventPublisher;
    private final EventProposalPersistencePort proposalPersistence;

    private final IntentParser intentParser = new IntentParser();
    private final ConflictDetector conflictDetector = new ConflictDetector();
    private final NaturalDateResolver dateResolver = new NaturalDateResolver();

    public AgentCommandService(AgentPersistencePort agentPersistence,
            CalendarPort calendarPort,
            LLMPort llmPort,
            ApprovalPort approvalPort,
            CoordinationProtocolPort coordinationProtocol,
            NotificationPort notificationPort,
            UserPersistencePort userPersistence,
            DomainEventPublisher eventPublisher,
            EventProposalPersistencePort proposalPersistence) {
        this.agentPersistence = agentPersistence;
        this.calendarPort = calendarPort;
        this.llmPort = llmPort;
        this.approvalPort = approvalPort;
        this.coordinationProtocol = coordinationProtocol;
        this.notificationPort = notificationPort;
        this.userPersistence = userPersistence;
        this.eventPublisher = eventPublisher;
        this.proposalPersistence = proposalPersistence;
    }

    @Override
    public void handleMessage(AgentId agentId, String rawText) {
        Agent agent = loadAgent(agentId);

        // Tier 1: rule-based parsing
        ParsedIntent intent = intentParser.parse(rawText);
        log.info("[IntentParser] Parsed intent={} from text=\"{}\"", intent.type(), rawText);

        // Tier 2: LLM fallback for UNKNOWN
        if (intent.type() == IntentType.UNKNOWN) {
            log.info("[IntentParser] Tier 1 returned UNKNOWN, falling back to LLM");
            Optional<String> llmResult = llmPort.classifyIntent(agentId, rawText);
            log.info("[LLMAdapter] LLM classified intent={}", llmResult.orElse("EMPTY"));
            if (llmResult.isPresent() && !"UNKNOWN".equalsIgnoreCase(llmResult.get())) {
                try {
                    IntentType llmType = IntentType.valueOf(llmResult.get().toUpperCase());
                    intent = new ParsedIntent(llmType, rawText, intent.params());
                    log.info("[IntentParser] LLM resolved to intent={}", llmType);
                } catch (IllegalArgumentException e) {
                    log.warn("[IntentParser] LLM returned unrecognized intent: {}", llmResult.get());
                }
            }
        }

        if (intent.type() == IntentType.UNKNOWN) {
            sendUnknownIntentReply(agent);
            return;
        }

        routeIntent(agent, intent);
    }

    private void routeIntent(Agent agent, ParsedIntent intent) {
        log.info("[EventService] Routing intent={} for agent={}", intent.type(), agent.getAgentId());
        switch (intent.type()) {
            case VIEW_SCHEDULE -> notifySchedule(agent);
            case ADD_EVENT -> requestPersonalEventApproval(agent, intent);
            case SCHEDULE_WITH -> {
                try {
                    initiateCollaboration(agent, intent);
                } catch (Exception e) {
                    log.warn("[AgentService] Collaboration initiation failed: {}", e.getMessage());
                    User user = userPersistence.findById(agent.getUserId()).orElse(null);
                    if (user != null) {
                        notificationPort.sendMessage(
                                user.getSlackIdentity().slackUserId(),
                                user.getSlackIdentity().workspaceId(),
                                "❌ Failed to schedule meeting: " + e.getMessage());
                    }
                }
            }
            default -> sendUnknownIntentReply(agent);
        }
    }

    @Override
    public List<TimeSlot> viewSchedule(AgentId agentId, TimeRange range) {
        return calendarPort.getEvents(agentId, range);
    }

    @Override
    public EventId createPersonalEvent(AgentId agentId, String title, TimeSlot timeSlot) {
        Agent agent = loadAgent(agentId);
        // Conflict detection
        TimeRange range = TimeRange.of(
                LocalDate.ofInstant(timeSlot.start(), ZoneOffset.UTC),
                LocalDate.ofInstant(timeSlot.end(), ZoneOffset.UTC));
        List<TimeSlot> existing = calendarPort.getEvents(agentId, range);

        if (conflictDetector.hasConflict(existing, timeSlot)) {
            log.warn("[EventService] Conflict detected for agent={} at {}", agentId, timeSlot);
            throw new IllegalStateException("Time slot conflicts with existing event for agent " + agentId);
        }

        log.info("[CalendarAdapter] Creating event in Google Calendar: {} @ {}", title, timeSlot.start());
        EventId eventId = calendarPort.createEvent(agentId, timeSlot, title);
        log.info("[CalendarAdapter] Event created successfully id={}", eventId.value());

        eventPublisher.publish(PersonalEventCreated.of(agentId, agent.getUserId(), eventId, timeSlot, title));
        return eventId;
    }

    /**
     * Fetches upcoming events and sends a formatted schedule summary via Slack.
     * Times are displayed in Indian Standard Time (IST, UTC+05:30).
     */
    private void notifySchedule(Agent agent) {
        TimeRange nextWeek = TimeRange.of(LocalDate.now(), LocalDate.now().plusDays(7));
        List<CalendarEvent> events = calendarPort.getCalendarEvents(agent.getAgentId(), nextWeek);

        java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
        java.time.format.DateTimeFormatter dayFmt = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd");
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

        StringBuilder sb = new StringBuilder();
        sb.append("📅 *Your upcoming schedule (next 7 days):*\n\n");

        if (events.isEmpty()) {
            sb.append("No events scheduled.\n");
        } else {
            for (CalendarEvent event : events) {
                java.time.ZonedDateTime startZdt = event.slot().start().atZone(ist);
                java.time.ZonedDateTime endZdt = event.slot().end().atZone(ist);
                sb.append("• *").append(event.title()).append("*\n");
                sb.append("   ").append(startZdt.format(dayFmt))
                        .append(", ").append(startZdt.format(timeFmt))
                        .append(" – ").append(endZdt.format(timeFmt)).append("\n\n");
            }
        }

        User user = userPersistence.findById(agent.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found for agent: " + agent.getAgentId()));
        notificationPort.sendMessage(
                user.getSlackIdentity().slackUserId(),
                user.getSlackIdentity().workspaceId(),
                sb.toString());
    }

    // ── Add Event Flow (State Machine) ─────────────────────────

    /**
     * Creates a personal event proposal with state machine transitions,
     * resolves natural-language dates, persists the proposal,
     * and sends an interactive Slack approval card.
     */
    private void requestPersonalEventApproval(Agent agent, ParsedIntent intent) {
        String title = intent.param("title");
        String dateTimeText = intent.param("dateTime");

        log.info("[IntentParser] Parsed title={} dateTime={}", title, dateTimeText);

        // Step 1: Create proposal → INITIATED
        EventProposalId proposalId = EventProposalId.generate();

        // Step 2: Resolve natural language date → actual Instant
        Instant eventStart = null;
        Instant eventEnd = null;

        if (dateTimeText != null && !dateTimeText.isEmpty()) {
            Optional<Instant> resolved = dateResolver.resolve(dateTimeText);
            if (resolved.isPresent()) {
                eventStart = resolved.get();
                eventEnd = eventStart.plusSeconds(3600); // default 1-hour event
            }
        }

        if (eventStart == null) {
            // Default to tomorrow at 10 AM IST if no date could be parsed
            log.warn("[DateResolver] Could not resolve date, defaulting to tomorrow 10:00 AM");
            java.time.LocalDateTime tomorrow = java.time.LocalDateTime.of(
                    LocalDate.now().plusDays(1), java.time.LocalTime.of(10, 0));
            eventStart = tomorrow.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
            eventEnd = eventStart.plusSeconds(3600);
        }

        EventProposal proposal = new EventProposal(
                proposalId, agent.getAgentId(),
                title != null ? title : "Event",
                eventStart, eventEnd);
        log.info("[EventService] Proposal created id={} status=INITIATED", proposalId);

        // Step 3: → PROPOSAL_GENERATED
        proposal.transitionTo(EventProposalStatus.PROPOSAL_GENERATED);
        log.info("[EventService] Proposal {} → PROPOSAL_GENERATED", proposalId);

        // Step 4: Request approval
        ApprovalId approvalId = approvalPort.requestPersonalApproval(
                agent.getAgentId(), "Create event: " + title, Duration.ofHours(12));
        proposal.linkApproval(approvalId);
        log.info("[ApprovalService] Approval created id={} for proposal {}", approvalId, proposalId);

        // Step 5: → AWAITING_USER_APPROVAL
        proposal.transitionTo(EventProposalStatus.AWAITING_USER_APPROVAL);
        log.info("[EventService] Proposal {} → AWAITING_USER_APPROVAL", proposalId);

        // Persist the proposal
        proposalPersistence.save(proposal);

        // Step 6: Send interactive Slack card with resolved date
        String proposalText = "📅 *Create Event:* " + (title != null ? title : "(No title)")
                + "\n🕐 " + dateResolver.formatRange(eventStart, eventEnd);

        User user = userPersistence.findById(agent.getUserId())
                .orElseThrow(() -> new NoSuchElementException("User not found for agent: " + agent.getAgentId()));
        notificationPort.sendApprovalRequest(
                user.getSlackIdentity().slackUserId(),
                user.getSlackIdentity().workspaceId(),
                proposalText,
                approvalId.value().toString());
        log.info("[NotificationService] Approval card sent to user for proposal {}", proposalId);
    }

    /**
     * Called when a personal approval is decided (via domain event).
     * Drives the state machine: APPROVED → EVENT_CREATED → COMPLETED
     * or REJECTED (terminal).
     */
    public void onPersonalApprovalDecided(ApprovalId approvalId, String decision, String userId) {
        log.info("[ApprovalService] Approval {} decided={}", approvalId, decision);

        // Load the proposal by approvalId
        Optional<EventProposal> proposalOpt = proposalPersistence.findByApprovalId(approvalId);
        if (proposalOpt.isEmpty()) {
            log.warn("[EventService] No proposal found for approval={}", approvalId);
            return;
        }

        EventProposal proposal = proposalOpt.get();
        Agent agent = loadAgent(proposal.getAgentId());
        User user = userPersistence.findById(agent.getUserId()).orElse(null);
        if (user == null) {
            log.warn("[EventService] No user found for agent={}", proposal.getAgentId());
            return;
        }

        if ("APPROVED".equals(decision)) {
            // → APPROVED
            proposal.transitionTo(EventProposalStatus.APPROVED);
            proposalPersistence.save(proposal);
            log.info("[EventService] Proposal {} → APPROVED", proposal.getProposalId());

            try {
                // → EVENT_CREATED (call Google Calendar)
                TimeSlot timeSlot = new TimeSlot(proposal.getStartTime(), proposal.getEndTime());
                EventId eventId = createPersonalEvent(proposal.getAgentId(), proposal.getTitle(), timeSlot);
                proposal.recordEventId(eventId);
                proposal.transitionTo(EventProposalStatus.EVENT_CREATED);
                proposalPersistence.save(proposal);
                log.info("[EventService] Proposal {} → EVENT_CREATED eventId={}", proposal.getProposalId(),
                        eventId.value());

                // → COMPLETED + send confirmation
                proposal.transitionTo(EventProposalStatus.COMPLETED);
                proposalPersistence.save(proposal);
                log.info("[EventService] Proposal {} → COMPLETED", proposal.getProposalId());

                String confirmMsg = "✅ *Event Created Successfully!*\n\n"
                        + "📌 *" + proposal.getTitle() + "*\n"
                        + "🕐 " + dateResolver.formatRange(proposal.getStartTime(), proposal.getEndTime());
                notificationPort.sendMessage(
                        user.getSlackIdentity().slackUserId(),
                        user.getSlackIdentity().workspaceId(),
                        confirmMsg);
                log.info("[NotificationService] Success notification sent for proposal {}", proposal.getProposalId());

            } catch (Exception e) {
                // → FAILED
                proposal.transitionTo(EventProposalStatus.FAILED);
                proposalPersistence.save(proposal);
                log.error("[EventService] Proposal {} → FAILED: {}", proposal.getProposalId(), e.getMessage(), e);

                notificationPort.sendMessage(
                        user.getSlackIdentity().slackUserId(),
                        user.getSlackIdentity().workspaceId(),
                        "❌ Failed to create event: " + e.getMessage());
            }
        } else {
            // → REJECTED
            proposal.transitionTo(EventProposalStatus.REJECTED);
            proposalPersistence.save(proposal);
            log.info("[EventService] Proposal {} → REJECTED", proposal.getProposalId());

            notificationPort.sendMessage(
                    user.getSlackIdentity().slackUserId(),
                    user.getSlackIdentity().workspaceId(),
                    "🚫 Event *" + proposal.getTitle() + "* was rejected.");
            log.info("[NotificationService] Rejection notification sent for proposal {}", proposal.getProposalId());
        }
    }

    // ── Collaboration ──

    /**
     * Resolves the target user from an @mention, finds their agent,
     * and initiates the A2A coordination protocol.
     * After coordination initiation, sends a slot selection card to the invitee.
     */
    private void initiateCollaboration(Agent agent, ParsedIntent intent) {
        String targetUsername = intent.param("targetUser");
        log.info("[AgentService] Initiating collaboration: requester agent={} target=@{}", agent.getAgentId(),
                targetUsername);

        if (targetUsername == null || targetUsername.isEmpty()) {
            User user = userPersistence.findById(agent.getUserId()).orElse(null);
            if (user != null) {
                notificationPort.sendMessage(
                        user.getSlackIdentity().slackUserId(),
                        user.getSlackIdentity().workspaceId(),
                        "❌ Could not identify the target user. Please mention them with @username.");
            }
            return;
        }

        // Resolve target user → User → Agent
        // targetUser can be either "username" or "slack:U_SLACK_ID"
        Optional<User> targetUserOpt;
        if (targetUsername.startsWith("slack:")) {
            // Resolve by Slack user ID
            String slackId = targetUsername.substring("slack:".length());
            User requesterUser = userPersistence.findById(agent.getUserId()).orElse(null);
            if (requesterUser == null) {
                log.warn("[AgentService] No requester user found for agent={}", agent.getAgentId());
                return;
            }
            WorkspaceId workspaceId = requesterUser.getSlackIdentity().workspaceId();
            targetUserOpt = userPersistence.findBySlackUserId(new SlackUserId(slackId), workspaceId);
            log.info("[AgentService] Resolved slack:{} → user={}", slackId,
                    targetUserOpt.map(u -> u.getUserId().toString()).orElse("NOT_FOUND"));
        } else {
            targetUserOpt = userPersistence.findByUsername(targetUsername);
        }
        if (targetUserOpt.isEmpty()) {
            User requesterUser = userPersistence.findById(agent.getUserId()).orElse(null);
            if (requesterUser != null) {
                notificationPort.sendMessage(
                        requesterUser.getSlackIdentity().slackUserId(),
                        requesterUser.getSlackIdentity().workspaceId(),
                        "❌ User *" + targetUsername + "* is not registered with CoAgent4U.");
            }
            return;
        }

        User targetUser = targetUserOpt.get();
        Optional<Agent> targetAgentOpt = agentPersistence.findByUserId(targetUser.getUserId());
        if (targetAgentOpt.isEmpty()) {
            User requesterUser = userPersistence.findById(agent.getUserId()).orElse(null);
            if (requesterUser != null) {
                notificationPort.sendMessage(
                        requesterUser.getSlackIdentity().slackUserId(),
                        requesterUser.getSlackIdentity().workspaceId(),
                        "❌ *" + targetUsername + "* does not have an active agent. Ask them to register first.");
            }
            return;
        }

        AgentId inviteeAgentId = targetAgentOpt.get().getAgentId();

        // Resolve date — default to tomorrow if not specified
        String dateText = intent.param("dateTime");
        java.time.LocalDate targetDate;
        if (dateText != null && !dateText.isEmpty()) {
            Optional<java.time.Instant> resolved = dateResolver.resolve(dateText);
            if (resolved.isPresent()) {
                targetDate = resolved.get().atZone(java.time.ZoneId.of("Asia/Kolkata")).toLocalDate();
            } else {
                targetDate = LocalDate.now().plusDays(1);
            }
        } else {
            targetDate = LocalDate.now().plusDays(1);
        }
        log.info("[AgentService] Resolved coordination date: {}", targetDate);

        TimeRange lookAhead = TimeRange.of(targetDate, targetDate);

        // Initiate coordination (generates slots, matches availability →
        // PROPOSAL_GENERATED)
        log.info("[AgentService] Calling coordinationProtocol.initiate()");
        com.coagent4u.shared.CoordinationId coordId = coordinationProtocol.initiate(
                agent.getAgentId(), inviteeAgentId,
                lookAhead, 60, "Meeting", "Asia/Kolkata");
        log.info("[AgentService] Coordination initiated id={}", coordId);

        // Get available slots and send selection card to invitee
        java.util.List<TimeSlot> availableSlots = coordinationProtocol.getAvailableSlots(coordId);

        if (availableSlots.isEmpty()) {
            // No slots available — notify requester
            User requesterUser = userPersistence.findById(agent.getUserId()).orElse(null);
            if (requesterUser != null) {
                notificationPort.sendMessage(
                        requesterUser.getSlackIdentity().slackUserId(),
                        requesterUser.getSlackIdentity().workspaceId(),
                        "❌ *" + targetUsername + "* is busy during all office hours on " + targetDate
                                + ". No available slots found.");
            }
            log.info("[NotificationService] No-slots notification sent to requester");
            return;
        }

        // Build requester mention for the slot card
        User requesterUser = userPersistence.findById(agent.getUserId()).orElse(null);
        String requesterMention = requesterUser != null
                ? "<@" + requesterUser.getSlackIdentity().slackUserId().value() + ">"
                : "Someone";

        // Send slot selection card to invitee (User B picks the slot)
        notificationPort.sendSlotSelection(
                targetUser.getSlackIdentity().slackUserId(),
                targetUser.getSlackIdentity().workspaceId(),
                coordId.value().toString(),
                availableSlots,
                requesterMention);
        log.info("[NotificationService] Slot selection card sent to invitee @{} with {} slots",
                targetUsername, availableSlots.size());

        // Notify requester that coordination is in progress
        if (requesterUser != null) {
            notificationPort.sendMessage(
                    requesterUser.getSlackIdentity().slackUserId(),
                    requesterUser.getSlackIdentity().workspaceId(),
                    "🔄 Scheduling in progress! Sent " + availableSlots.size()
                            + " time slot options to <@" + targetUser.getSlackIdentity().slackUserId().value()
                            + "> for " + targetDate + ". Waiting for their selection.");
        }
    }

    // ── Fallback ──

    private void sendUnknownIntentReply(Agent agent) {
        log.info("[EventService] Unknown intent for agent={}, sending fallback", agent.getAgentId());
        User user = userPersistence.findById(agent.getUserId()).orElse(null);
        if (user != null) {
            notificationPort.sendMessage(
                    user.getSlackIdentity().slackUserId(),
                    user.getSlackIdentity().workspaceId(),
                    "🤔 I didn't understand that. Try:\n"
                            + "• `show my schedule`\n"
                            + "• `add event <title> on <date> at <time>`\n"
                            + "• `schedule a meeting with @user`");
        }
    }

    private Agent loadAgent(AgentId agentId) {
        return agentPersistence.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("Agent not found: " + agentId));
    }
}
