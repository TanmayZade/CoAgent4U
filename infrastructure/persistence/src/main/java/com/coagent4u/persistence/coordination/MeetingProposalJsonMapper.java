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
 * <li>Audit-safe output (human-readable, no class metadata)</li>
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
            Map<String, Object> map = Map.of(
                    "proposalId", p.proposalId(),
                    "requesterAgentId", p.requesterAgentId().value().toString(),
                    "inviteeAgentId", p.inviteeAgentId().value().toString(),
                    "suggestedStart", p.suggestedTime().start().toString(),
                    "suggestedEnd", p.suggestedTime().end().toString(),
                    "durationMinutes", p.durationMinutes(),
                    "title", p.title(),
                    "timezone", p.timezone());
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
                    new AgentId(UUID.fromString((String) map.get("requesterAgentId"))),
                    new AgentId(UUID.fromString((String) map.get("inviteeAgentId"))),
                    new TimeSlot(
                            Instant.parse((String) map.get("suggestedStart")),
                            Instant.parse((String) map.get("suggestedEnd"))),
                    ((Number) map.get("durationMinutes")).intValue(),
                    (String) map.get("title"),
                    (String) map.get("timezone"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize MeetingProposal", e);
        }
    }
}
