package com.coagent4u.a2a.adapter.in.rpc;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard JSON-RPC 2.0 Request model for Google A2A protocol.
 */
public record RPCRequest(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("method") String method,
    @JsonProperty("params") Map<String, Object> params,
    @JsonProperty("id") Object id
) {}
