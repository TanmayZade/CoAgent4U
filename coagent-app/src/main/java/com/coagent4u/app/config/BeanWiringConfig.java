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
            CreateApprovalUseCase createApprovalUseCase) {
        return new AgentApprovalPortImpl(agentPersistence, createApprovalUseCase);
    }

    @Bean
    ApprovalPortAdapter approvalPort(
            AgentPersistencePort agentPersistence,
            CreateApprovalUseCase createApprovalUseCase) {
        return new ApprovalPortAdapter(agentPersistence, createApprovalUseCase);
    }
}
