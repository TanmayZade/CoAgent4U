package com.coagent4u.persistence.coordination;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeSlot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Deterministic JSON serialization for MeetingProposal JSONB column.
 * <ul>
 * <li>No default typing</li>
 * <li>No polymorphic type magic</li>
 * <li>Explicit field mapping</li>
 * <li>AgentActivity-safe output (human-readable, no class metadata)</li>
 * </ul>
 */
public final class MeetingProposalJsonMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private MeetingProposalJsonMapper() {
    }

    @SuppressWarnings("unchecked")
    public static String toJson(MeetingProposal p) {
        try {
            // Explicit field mapping — no @JsonTypeInfo, no automatic type detection
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("proposalId", p.proposalId());
            map.put("coordinationIdStr", p.coordinationIdStr());
            map.put("requesterAgentId", p.requesterAgentId().value().toString());
            map.put("inviteeAgentId", p.inviteeAgentId().value().toString());
            map.put("suggestedStart", p.suggestedTime().start().toString());
            map.put("suggestedEnd", p.suggestedTime().end().toString());
            map.put("durationMinutes", p.durationMinutes());
            map.put("title", p.title());
            map.put("timezone", p.timezone());
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize MeetingProposal", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static MeetingProposal fromJson(String json) {
        try {
            Map<String, Object> map = MAPPER.readValue(json, Map.class);
            return new MeetingProposal(
                    (String) map.get("proposalId"),
                    (String) map.getOrDefault("coordinationIdStr", ""),
                    new AgentId(UUID.fromString((String) map.get("requesterAgentId"))),
                    new AgentId(UUID.fromString((String) map.get("inviteeAgentId"))),
                    new TimeSlot(
                            Instant.parse((String) map.get("suggestedStart")),
                            Instant.parse((String) map.get("suggestedEnd"))),
                    ((Number) map.get("durationMinutes")).intValue(),
                    (String) map.getOrDefault("title", "Coordination Sync"),
                    (String) map.getOrDefault("timezone", "UTC"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize MeetingProposal", e);
        }
    }
}
