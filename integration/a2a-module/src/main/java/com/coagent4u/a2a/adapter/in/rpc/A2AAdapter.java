package com.coagent4u.a2a.adapter.in.rpc;

import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter that translates Google A2A JSON-RPC calls into Coordination Engine commands.
 */
@Service
public class A2AAdapter {

    private static final Logger log = LoggerFactory.getLogger(A2AAdapter.class);
    private final CoordinationProtocolPort coordinationPort;

    public A2AAdapter(CoordinationProtocolPort coordinationPort) {
        this.coordinationPort = coordinationPort;
    }

    public RPCResponse dispatch(RPCRequest request) {
        log.info("[A2AAdapter] Dispatching method: {}", request.method());

        try {
            return switch (request.method()) {
                case "propose_governance_session" -> handleProposeSession(request);
                case "select_governance_consensus" -> handleSelectConsensus(request);
                case "terminate_session" -> handleTerminateSession(request);
                default -> RPCResponse.error(request.id(), -32601, "Method not found: " + request.method());
            };
        } catch (Exception e) {
            log.error("[A2AAdapter] Error handling A2A request", e);
            return RPCResponse.error(request.id(), -32000, "Internal error: " + e.getMessage());
        }
    }

    private RPCResponse handleProposeSession(RPCRequest request) {
        Map<String, Object> params = request.params();
        
        // Map A2A params to our Coordination initiation
        AgentId requester = new AgentId(UUID.fromString((String) params.get("requester_id")));
        AgentId invitee = new AgentId(UUID.fromString((String) params.get("invitee_id")));
        int duration = (Integer) params.get("duration_minutes");
        String title = (String) params.get("title");
        
        // Simple 7-day lookahead for now
        TimeRange range = new TimeRange(LocalDate.now(), LocalDate.now().plusDays(7));
        
        CoordinationId coordId = CoordinationId.generate();
        CorrelationId corrId = CorrelationId.generate();

        coordinationPort.initiate(coordId, corrId, requester, invitee, range, duration, title, "Asia/Kolkata");

        return RPCResponse.success(request.id(), Map.of("session_id", coordId.value().toString(), "status", "INITIATED"));
    }

    private RPCResponse handleSelectConsensus(RPCRequest request) {
        // Implementation for selecting a slot/consensus item
        return RPCResponse.success(request.id(), Map.of("status", "SUCCESS"));
    }

    private RPCResponse handleTerminateSession(RPCRequest request) {
        // Implementation for terminating a session
        return RPCResponse.success(request.id(), Map.of("status", "TERMINATED"));
    }
}
