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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

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
            DomainEventPublisher eventPublisher,
            java.util.List<com.coagent4u.coordination.domain.policy.GovernancePolicy> governancePolicies) {
        return new CoordinationService(persistence,
                agentAvailabilityPort, agentEventExecutionPort,
                agentProfilePort, agentApprovalPort, eventPublisher, governancePolicies);
    }

    @Bean
    com.coagent4u.coordination.domain.policy.GovernancePolicy autoApprovalPolicy() {
        // Auto-approve meetings shorter than 31 minutes
        return new com.coagent4u.coordination.domain.policy.AutoApprovalPolicy(30);
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
            com.coagent4u.agent.port.out.EventProposalPersistencePort proposalPersistence,
            com.coagent4u.agent.port.out.PythonAgentPort pythonAgentPort) {
        return new AgentCommandService(agentPersistence,
                calendarPort, llmPort, approvalPort,
                coordinationProtocol, notificationPort, userPersistence, eventPublisher,
                proposalPersistence, pythonAgentPort);
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
    public Cache<String, com.coagent4u.shared.AgentId> agentIdCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean
    public Cache<com.coagent4u.shared.AgentId, com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile> agentProfileCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Bean
    com.coagent4u.coordination.application.CoordinationQueryService.UserAgentResolver userAgentResolver(
            AgentPersistencePort agentPersistence,
            UserQueryPort userQuery,
            Cache<String, com.coagent4u.shared.AgentId> agentIdCache,
            Cache<com.coagent4u.shared.AgentId, com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile> profileCache) {
        // Bridge: resolves username ↔ AgentId without coupling modules
        return new com.coagent4u.coordination.application.CoordinationQueryService.UserAgentResolver() {
            @Override
            public java.util.Optional<com.coagent4u.shared.AgentId> resolveAgentId(String username) {
                com.coagent4u.shared.AgentId cached = agentIdCache.getIfPresent(username);
                if (cached != null) return java.util.Optional.of(cached);

                java.util.Optional<com.coagent4u.shared.AgentId> resolved = userQuery.findByUsername(username)
                        .flatMap(user -> agentPersistence.findByUserId(user.getUserId()))
                        .map(agent -> agent.getAgentId());
                
                resolved.ifPresent(id -> agentIdCache.put(username, id));
                return resolved;
            }

            @Override
            public com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile resolveProfile(com.coagent4u.shared.AgentId agentId) {
                return profileCache.get(agentId, id -> agentPersistence.findById(id)
                        .flatMap(agent -> userQuery.findById(agent.getUserId()))
                        .map(user -> new com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile(
                                user.getUsername(),
                                user.getSlackIdentity() != null ? user.getSlackIdentity().displayName() : null,
                                user.getSlackIdentity() != null ? user.getSlackIdentity().avatarUrl() : null
                        ))
                        .orElse(new com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile("unknown", null, null)));
            }

            @Override
            public java.util.Map<com.coagent4u.shared.AgentId, com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile> resolveProfiles(java.util.Collection<com.coagent4u.shared.AgentId> agentIds) {
                if (agentIds.isEmpty()) return java.util.Map.of();

                java.util.Map<com.coagent4u.shared.AgentId, com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile> result = new java.util.HashMap<>();
                java.util.List<com.coagent4u.shared.AgentId> toFetch = new java.util.ArrayList<>();

                for (com.coagent4u.shared.AgentId id : agentIds) {
                    com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile cached = profileCache.getIfPresent(id);
                    if (cached != null) {
                        result.put(id, cached);
                    } else {
                        toFetch.add(id);
                    }
                }

                if (!toFetch.isEmpty()) {
                    // Fetch all agents
                    java.util.List<com.coagent4u.agent.domain.Agent> agents = agentPersistence.findAllById(toFetch);
                    java.util.Map<com.coagent4u.shared.UserId, com.coagent4u.shared.AgentId> userToAgentMap = agents.stream()
                            .collect(java.util.stream.Collectors.toMap(com.coagent4u.agent.domain.Agent::getUserId, com.coagent4u.agent.domain.Agent::getAgentId));

                    // Fetch all users
                    java.util.List<com.coagent4u.user.domain.User> users = userQuery.findAllById(userToAgentMap.keySet());

                    // Map users back to profiles and put in cache
                    for (com.coagent4u.user.domain.User user : users) {
                        com.coagent4u.shared.AgentId aid = userToAgentMap.get(user.getUserId());
                        com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile p = new com.coagent4u.coordination.application.CoordinationQueryService.AgentProfile(
                                user.getUsername(),
                                user.getSlackIdentity() != null ? user.getSlackIdentity().displayName() : null,
                                user.getSlackIdentity() != null ? user.getSlackIdentity().avatarUrl() : null
                        );
                        result.put(aid, p);
                        profileCache.put(aid, p);
                    }
                }

                return result;
            }

            @Override
            public String resolveUsername(com.coagent4u.shared.AgentId agentId) {
                return resolveProfile(agentId).username();
            }

            @Override
            public String resolveDisplayName(com.coagent4u.shared.AgentId agentId) {
                return resolveProfile(agentId).displayName();
            }

            @Override
            public String resolveAvatarUrl(com.coagent4u.shared.AgentId agentId) {
                return resolveProfile(agentId).avatarUrl();
            }
        };
    }

    @Bean
    com.coagent4u.coordination.application.CoordinationQueryService coordinationQueryService(
            CoordinationPersistencePort coordinationPersistence,
            com.coagent4u.coordination.application.CoordinationQueryService.UserAgentResolver resolver) {
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
                new com.coagent4u.agent.application.DashboardService.CoordinationSummaryMapper() {
            @Override
            public java.util.List<com.coagent4u.coordination.application.dto.CoordinationSummary> mapRecent(com.coagent4u.shared.AgentId agentId, int limit) {
                return coordinationQueryService.getHistory(
                        // Resolve agentId back to username for the query service
                        agentPersistence.findById(agentId)
                                .flatMap(agent -> userQuery.findById(agent.getUserId()))
                                .map(user -> user.getUsername())
                                .orElse("unknown"),
                        null, 0, limit).content();
            }

            @Override
            public java.util.List<com.coagent4u.coordination.application.dto.CoordinationActivityPoint> mapActivity(com.coagent4u.shared.AgentId agentId, int days) {
                return coordinationQueryService.getActivityStats(agentId, days);
            }
        };

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
