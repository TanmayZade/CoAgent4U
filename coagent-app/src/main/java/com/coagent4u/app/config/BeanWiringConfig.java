package com.coagent4u.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.coagent4u.agent.application.AgentCommandService;
import com.coagent4u.agent.capability.AgentApprovalPortImpl;
import com.coagent4u.agent.capability.AgentAvailabilityPortImpl;
import com.coagent4u.agent.capability.AgentEventExecutionPortImpl;
import com.coagent4u.agent.capability.AgentProfilePortImpl;
import com.coagent4u.agent.capability.ApprovalPortAdapter;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.ApprovalPort;
import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.approval.application.ApprovalService;
import com.coagent4u.approval.port.in.CreateApprovalUseCase;
import com.coagent4u.approval.port.out.ApprovalPersistencePort;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.coordination.application.CoordinationService;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.user.application.UserManagementService;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Central Spring configuration that wires domain services and capability
 * bridges.
 *
 * <p>
 * Domain-module classes intentionally have zero Spring annotations (domain
 * purity).
 * This configuration assembles them into Spring beans, preserving hexagonal
 * architecture.
 * </p>
 */
@Configuration
public class BeanWiringConfig {

    // ── Core Domain Services ─────────────────────────────────

    @Bean
    UserManagementService userManagementService(
            UserPersistencePort persistence,
            DomainEventPublisher eventPublisher) {
        return new UserManagementService(persistence, eventPublisher);
    }

    @Bean
    ApprovalService approvalService(
            ApprovalPersistencePort persistence,
            DomainEventPublisher eventPublisher) {
        return new ApprovalService(persistence, eventPublisher);
    }

    @Bean
    CoordinationService coordinationService(
            CoordinationPersistencePort persistence,
            AgentAvailabilityPort agentAvailabilityPort,
            AgentEventExecutionPort agentEventExecutionPort,
            AgentProfilePort agentProfilePort,
            AgentApprovalPort agentApprovalPort,
            DomainEventPublisher eventPublisher) {
        return new CoordinationService(persistence,
                agentAvailabilityPort, agentEventExecutionPort,
                agentProfilePort, agentApprovalPort, eventPublisher);
    }

    @Bean
    AgentCommandService agentCommandService(
            AgentPersistencePort agentPersistence,
            CalendarPort calendarPort,
            LLMPort llmPort,
            ApprovalPort approvalPort,
            CoordinationProtocolPort coordinationProtocol,
            NotificationPort notificationPort,
            UserPersistencePort userPersistence,
            DomainEventPublisher eventPublisher,
            com.coagent4u.agent.port.out.EventProposalPersistencePort proposalPersistence) {
        return new AgentCommandService(agentPersistence,
                calendarPort, llmPort, approvalPort,
                coordinationProtocol, notificationPort, userPersistence, eventPublisher,
                proposalPersistence);
    }

    // ── Capability Bridges (agent-module → coordination ports) ──

    @Bean
    AgentAvailabilityPortImpl agentAvailabilityPort(CalendarPort calendarPort) {
        return new AgentAvailabilityPortImpl(calendarPort);
    }

    @Bean
    AgentEventExecutionPortImpl agentEventExecutionPort(CalendarPort calendarPort) {
        return new AgentEventExecutionPortImpl(calendarPort);
    }

    @Bean
    AgentProfilePortImpl agentProfilePort(
            AgentPersistencePort agentPersistence,
            UserQueryPort userQuery) {
        return new AgentProfilePortImpl(agentPersistence, userQuery);
    }

    @Bean
    AgentApprovalPortImpl agentApprovalPort(
            AgentPersistencePort agentPersistence,
            com.coagent4u.user.port.out.UserPersistencePort userPersistence,
            CreateApprovalUseCase createApprovalUseCase,
            com.coagent4u.user.port.out.NotificationPort notificationPort) {
        return new AgentApprovalPortImpl(agentPersistence, userPersistence, createApprovalUseCase, notificationPort);
    }

    @Bean
    ApprovalPortAdapter approvalPort(
            AgentPersistencePort agentPersistence,
            CreateApprovalUseCase createApprovalUseCase) {
        return new ApprovalPortAdapter(agentPersistence, createApprovalUseCase);
    }

    // ── Dashboard Query Services ─────────────────────────────────

    @Bean
    com.coagent4u.coordination.application.CoordinationQueryService coordinationQueryService(
            CoordinationPersistencePort coordinationPersistence,
            AgentPersistencePort agentPersistence,
            UserQueryPort userQuery) {

        // Bridge: resolves username ↔ AgentId without coupling modules
        com.coagent4u.coordination.application.CoordinationQueryService.UserAgentResolver resolver =
                new com.coagent4u.coordination.application.CoordinationQueryService.UserAgentResolver() {
                    @Override
                    public java.util.Optional<com.coagent4u.shared.AgentId> resolveAgentId(String username) {
                        return userQuery.findByUsername(username)
                                .flatMap(user -> agentPersistence.findByUserId(user.getUserId()))
                                .map(agent -> agent.getAgentId());
                    }

                    @Override
                    public String resolveUsername(com.coagent4u.shared.AgentId agentId) {
                        return agentPersistence.findById(agentId)
                                .flatMap(agent -> userQuery.findById(agent.getUserId()))
                                .map(user -> user.getUsername())
                                .orElse("unknown");
                    }
                };

        return new com.coagent4u.coordination.application.CoordinationQueryService(
                coordinationPersistence, resolver);
    }

    @Bean
    com.coagent4u.agent.application.DashboardService dashboardService(
            UserQueryPort userQuery,
            AgentPersistencePort agentPersistence,
            ApprovalPersistencePort approvalPersistence,
            com.coagent4u.coordination.application.CoordinationQueryService coordinationQueryService) {

        // Bridge: maps recent coordinations to summaries with resolved usernames
        com.coagent4u.agent.application.DashboardService.CoordinationSummaryMapper mapper =
                (agentId, limit) -> coordinationQueryService.getHistory(
                        // Resolve agentId back to username for the query service
                        agentPersistence.findById(agentId)
                                .flatMap(agent -> userQuery.findById(agent.getUserId()))
                                .map(user -> user.getUsername())
                                .orElse("unknown"),
                        null, 0, limit).content();

        return new com.coagent4u.agent.application.DashboardService(
                userQuery, agentPersistence,
                approvalPersistence, mapper);
    }

    // ── Priority 3-5: Agent Activity & Pending Approvals ──────────

    @Bean
    com.coagent4u.user.application.AgentActivityQueryService agentActivityQueryService(
            UserQueryPort userQuery,
            com.coagent4u.user.port.out.AgentActivityQueryPort agentActivityQuery) {
        return new com.coagent4u.user.application.AgentActivityQueryService(userQuery, agentActivityQuery);
    }

    @Bean
    com.coagent4u.approval.application.PendingApprovalsService pendingApprovalsService(
            UserQueryPort userQuery,
            ApprovalPersistencePort approvalPersistence) {

        // Bridge: username → UserId without coupling approval-module to user-module
        com.coagent4u.approval.application.PendingApprovalsService.UserIdResolver resolver =
                username -> userQuery.findByUsername(username)
                        .map(user -> user.getUserId());

        return new com.coagent4u.approval.application.PendingApprovalsService(
                resolver, approvalPersistence);
    }
}
