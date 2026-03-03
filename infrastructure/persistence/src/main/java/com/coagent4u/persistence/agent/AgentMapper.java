package com.coagent4u.persistence.agent;

import java.lang.reflect.Field;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Infrastructure-only mapper between Agent domain aggregate and JPA entity.
 * Uses reflection for toDomain since Agent has no pullDomainEvents() and
 * its public constructor always sets state=ACTIVE + generates timestamps.
 */
public final class AgentMapper {

    private AgentMapper() {
    }

    public static AgentJpaEntity toJpa(Agent agent) {
        AgentJpaEntity entity = new AgentJpaEntity();
        entity.setAgentId(agent.getAgentId().value());
        entity.setUserId(agent.getUserId().value());
        entity.setStatus(agent.getStatus().name());
        entity.setCreatedAt(agent.getCreatedAt());
        // AgentJpaEntity has no updatedAt column — createdAt only
        return entity;
    }

    /**
     * Reconstitutes Agent from JPA entity via reflection.
     * AgentJpaEntity has no updatedAt field; set to createdAt for domain
     * consistency.
     */
    public static Agent toDomain(AgentJpaEntity e) {
        try {
            Agent agent = new Agent(new AgentId(e.getAgentId()), new UserId(e.getUserId()));
            setField(agent, "status", Agent.Status.valueOf(e.getStatus()));
            setField(agent, "createdAt", e.getCreatedAt());
            setField(agent, "updatedAt", e.getCreatedAt()); // no updatedAt in DB
            return agent;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to reconstitute Agent from JPA entity", ex);
        }
    }

    private static void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
