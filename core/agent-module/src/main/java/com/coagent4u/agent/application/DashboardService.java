package com.coagent4u.agent.application;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.agent.application.dto.DashboardSummary;
import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.in.GetDashboardSummaryUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.approval.port.out.ApprovalPersistencePort;
import com.coagent4u.coordination.application.dto.CoordinationActivityPoint;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Application service for the main Dashboard view.
 * Orchestrates data from Agent, Coordination, and Approval modules.
 *
 * <p>No Spring annotations — assembled by DI in coagent-app.
 */
public class DashboardService implements GetDashboardSummaryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final UserQueryPort userQueryPort;
    private final AgentPersistencePort agentPersistence;
    private final ApprovalPersistencePort approvalPersistence;

    /**
     * Functional interface to resolve an AgentId to the other participant's username.
     * Avoids coupling this service to the coordination-module's internal resolution logic.
     */
    public interface CoordinationSummaryMapper {
        List<CoordinationSummary> mapRecent(com.coagent4u.shared.AgentId agentId, int limit);
        List<CoordinationActivityPoint> mapActivity(com.coagent4u.shared.AgentId agentId, int days);
    }

    private final CoordinationSummaryMapper summaryMapper;

    public DashboardService(UserQueryPort userQueryPort,
                            AgentPersistencePort agentPersistence,
                            ApprovalPersistencePort approvalPersistence,
                            CoordinationSummaryMapper summaryMapper) {
        this.userQueryPort = userQueryPort;
        this.agentPersistence = agentPersistence;
        this.approvalPersistence = approvalPersistence;
        this.summaryMapper = summaryMapper;
    }

    @Override
    public DashboardSummary getSummary(String username) {
        User user = userQueryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

        UserId userId = user.getUserId();

        Agent agent = agentPersistence.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No agent found for user: " + username));

        // Agent status
        boolean slackConnected = user.getSlackIdentity() != null;
        boolean gcalConnected = user.activeConnectionFor("GOOGLE_CALENDAR").isPresent();

        DashboardSummary.AgentStatusDto agentStatus = new DashboardSummary.AgentStatusDto(
                agent.getStatus().name(),
                slackConnected,
                gcalConnected,
                agent.getUpdatedAt());

        // Pending approvals count
        int pendingCount = approvalPersistence.findPendingByUser(userId).size();

        // Recent coordinations (last 5)
        List<CoordinationSummary> recentCoordinations = summaryMapper.mapRecent(agent.getAgentId(), 5);

        // Activity stats (last 7 days)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<CoordinationActivityPoint> rawStats = summaryMapper.mapActivity(agent.getAgentId(), 7);
        
        java.util.List<DashboardSummary.ActivityPointDto> activitySummary = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            CoordinationActivityPoint point = rawStats.stream()
                    .filter(p -> p.date().equals(date))
                    .findFirst()
                    .orElse(new CoordinationActivityPoint(date, 0, 0, 0));
            
            activitySummary.add(new DashboardSummary.ActivityPointDto(
                    point.date().toString(),
                    point.completed(),
                    point.rejected(),
                    point.failed()
            ));
        }

        log.info("[DashboardService] Summary for {}: agent={}, pending={}, recent={}, activity={}",
                username, agent.getStatus(), pendingCount, recentCoordinations.size(), activitySummary.size());

        return new DashboardSummary(agentStatus, pendingCount, recentCoordinations, activitySummary);
    }
}
