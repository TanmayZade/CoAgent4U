package com.coagent4u.a2a.adapter.in.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard JSON-RPC 2.0 Error model.
 */
public record RPCError(
    @JsonProperty("code") int code,
    @JsonProperty("message") String message,
    @JsonProperty("data") Object data
) {}
