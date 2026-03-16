package com.coagent4u.monitoring;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.events.*;
import com.coagent4u.persistence.agent.AgentJpaEntity;
import com.coagent4u.persistence.agent.AgentJpaRepository;
import com.coagent4u.persistence.coordination.CoordinationJpaRepository;
import com.coagent4u.persistence.user.UserJpaEntity;
import com.coagent4u.persistence.user.UserJpaRepository;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EventDescriptionMapper {

    private final UserJpaRepository userRepository;
    private final AgentJpaRepository agentRepository;
    private final CoordinationJpaRepository coordinationRepository;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Kolkata"));

    public EventDescriptionMapper(UserJpaRepository userRepository, 
                                  AgentJpaRepository agentRepository,
                                  CoordinationJpaRepository coordinationRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.coordinationRepository = coordinationRepository;
    }

    public List<MappedEvent> mapEvent(DomainEvent event) {
        List<MappedEvent> results = new ArrayList<>();
        String description = "Unknown event occurred";
        String level = "INFO";
        UUID correlationId = null;
        UUID coordinationId = null;
        UUID agentId = null;

        if (event instanceof CorrelationAwareEvent ce && ce.correlationId() != null) {
            correlationId = ce.correlationId().value();
        }
        if (event instanceof CoordinationAwareEvent coe && coe.coordinationId() != null) {
            coordinationId = coe.coordinationId().value();
        }

        switch (event) {
            case AgentActivated e -> {
                agentId = e.agentId().value();
                String msg = resolveMentions(e.rawText());
                description = "Agent activated via " + e.source() + " with message: \"" + msg + "\"";
                level = "INFO";
            }
            case IntentParsed e -> {
                agentId = e.agentId().value();
                description = "Successfully understood intent as " + e.intentType();
                level = "SUCCESS";
            }
            case LLMFallbackTriggered e -> {
                agentId = e.agentId().value();
                description = "Used LLM fallback to classify message. Result: " + e.llmResponse();
                level = "WARNING";
            }
            case UnrecognizedIntent e -> {
                agentId = e.agentId().value();
                String msg = resolveMentions(e.rawText());
                description = "Could not recognize intent for message: \"" + msg + "\"";
                level = "ERROR";
            }
            case ScheduleViewed e -> {
                agentId = e.agentId().value();
                description = "Viewed schedule (" + e.eventCount() + " events found)";
                level = "INFO";
            }
            case DateResolved e -> {
                agentId = e.agentId().value();
                description = "Recognized \"" + e.originalText() + "\" as " + timeFormatter.format(e.resolvedInstant());
                level = "INFO";
            }
            case PersonalApprovalRequested e -> {
                agentId = e.agentId().value();
                description = "Requested your approval to schedule an event";
                level = "SUCCESS";
            }
            case PersonalEventCreated e -> {
                agentId = e.agentId().value();
                description = "Successfully created calendar event: \"" + e.title() + "\"";
                level = "SUCCESS";
            }
            case PersonalEventFailed e -> {
                agentId = e.agentId().value();
                description = "Failed to create event \"" + e.title() + "\": " + e.errorMessage();
                level = "ERROR";
            }
            case TaskCompleted e -> {
                agentId = e.agentId().value();
                description = "Task completed (" + e.taskType() + "): " + e.summary();
                level = "SUCCESS";
            }
            case TaskFailed e -> {
                agentId = e.agentId().value();
                description = "Task failed (" + e.taskType() + "): " + e.errorMessage();
                level = "ERROR";
            }
            case CoordinationInitiated e -> {
                agentId = e.agentId().value();
                String targetName = resolveUsername(e.targetUserId().value());
                description = "Initiated coordination with @" + targetName;
                level = "INFO";
            }
            case CoordinationRequestReceived e -> {
                agentId = e.agentId().value();
                String requesterName = resolveUsername(e.requesterUserId().value());
                description = "Received meeting request from @" + requesterName;
                level = "INFO";
            }
            case CalendarSourced e -> {
                agentId = e.agentId().value();
                description = "Sourced calendar availability (" + e.eventCount() + " existing events)";
                level = "INFO";
            }
            case ConflictDetected e -> {
                agentId = e.agentId().value();
                description = "Scheduling conflict detected: " + e.conflictReason();
                level = "WARNING";
            }
            case SlotsProposed e -> {
                agentId = e.agentId().value();
                description = "Proposed " + e.slotCount() + " available time slots";
                level = "SUCCESS";
            }
            case SlotsReceived e -> {
                agentId = e.agentId().value();
                description = "Received " + e.slotCount() + " proposed time slots";
                level = "SUCCESS";
            }
            case AgentProvisioned e -> {
                agentId = e.agentId().value();
                description = "Agent provisioned";
                level = "SUCCESS";
            }
            case ApprovalDecisionMade e -> {
                Optional<AgentJpaEntity> a = agentRepository.findByUserId(e.userId().value());
                if (a.isPresent()) {
                    agentId = a.get().getAgentId();
                    description = e.approvalType() + " approval decision made: " + e.decision();
                    level = "APPROVED".equals(e.decision()) ? "SUCCESS" : "WARNING";
                }
            }
            case ApprovalExpired e -> {
                Optional<AgentJpaEntity> a = agentRepository.findByUserId(e.userId().value());
                if (a.isPresent()) {
                    agentId = a.get().getAgentId();
                    description = e.approvalType() + " approval expired";
                    level = "WARNING";
                }
            }
            case CoordinationStateChanged e -> {
                final UUID finalCorrelationId = correlationId;
                final UUID finalCoordinationId = coordinationId;
                coordinationRepository.findById(e.coordinationId().value()).ifPresent(c -> {
                    String desc = "Coordination state changed to " + e.toState() + " (" + e.reason() + ")";
                    String lvl = "INFO";
                    results.add(new MappedEvent(desc, lvl, c.getRequesterAgentId(), finalCorrelationId, finalCoordinationId));
                    results.add(new MappedEvent(desc, lvl, c.getInviteeAgentId(), finalCorrelationId, finalCoordinationId));
                });
                return results; // Return early for multiple agent insertion
            }
            case CoordinationCompleted e -> {
                final UUID finalCorrelationId = correlationId;
                final UUID finalCoordinationId = coordinationId;
                coordinationRepository.findById(e.coordinationId().value()).ifPresent(c -> {
                    String desc = "Coordination completed successfully";
                    String lvl = "SUCCESS";
                    results.add(new MappedEvent(desc, lvl, c.getRequesterAgentId(), finalCorrelationId, finalCoordinationId));
                    results.add(new MappedEvent(desc, lvl, c.getInviteeAgentId(), finalCorrelationId, finalCoordinationId));
                });
                return results;
            }
            case CoordinationFailed e -> {
                final UUID finalCorrelationId = correlationId;
                final UUID finalCoordinationId = coordinationId;
                coordinationRepository.findById(e.coordinationId().value()).ifPresent(c -> {
                    String desc = "Coordination failed: " + e.reason();
                    String lvl = "ERROR";
                    results.add(new MappedEvent(desc, lvl, c.getRequesterAgentId(), finalCorrelationId, finalCoordinationId));
                    results.add(new MappedEvent(desc, lvl, c.getInviteeAgentId(), finalCorrelationId, finalCoordinationId));
                });
                return results;
            }
            default -> {
                description = "Event occurred: " + event.getClass().getSimpleName();
            }
        }

        if (agentId != null) {
            results.add(new MappedEvent(description, level, agentId, correlationId, coordinationId));
        }
        return results;
    }

    private String resolveUsername(UUID userId) {
        return userRepository.findById(userId)
                .map(UserJpaEntity::getUsername)
                .orElse("Unknown");
    }

    private String resolveMentions(String text) {
        if (text == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("slack:([A-Z0-9]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String slackId = matcher.group(1);
            String displayName = userRepository.findFirstBySlackIdentity_SlackUserId(slackId)
                    .map(u -> {
                        if (u.getSlackIdentity() != null && u.getSlackIdentity().getDisplayName() != null) {
                            return u.getSlackIdentity().getDisplayName();
                        }
                        return u.getUsername();
                    })
                    .orElse(slackId);
            matcher.appendReplacement(sb, "@" + displayName);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public record MappedEvent(String description, String level, UUID agentId, UUID correlationId, UUID coordinationId) {}
}
