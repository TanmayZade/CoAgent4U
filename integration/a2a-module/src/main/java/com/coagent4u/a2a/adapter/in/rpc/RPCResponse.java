package com.coagent4u.a2a.adapter.in.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard JSON-RPC 2.0 Response model for Google A2A protocol.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RPCResponse(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("result") Object result,
    @JsonProperty("error") RPCError error,
    @JsonProperty("id") Object id
) {
    public static RPCResponse success(Object id, Object result) {
        return new RPCResponse("2.0", result, null, id);
    }

    public static RPCResponse error(Object id, int code, String message) {
        return new RPCResponse("2.0", null, new RPCError(code, message, null), id);
    }
}
