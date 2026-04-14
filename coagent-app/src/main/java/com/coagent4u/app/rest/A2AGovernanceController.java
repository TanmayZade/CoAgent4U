package com.coagent4u.app.rest;

import com.coagent4u.a2a.adapter.in.rpc.A2AAdapter;
import com.coagent4u.a2a.adapter.in.rpc.RPCRequest;
import com.coagent4u.a2a.adapter.in.rpc.RPCResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for exposure of Google A2A Protocol via JSON-RPC.
 */
@RestController
@RequestMapping("/v1/a2a")
public class A2AGovernanceController {

    private final A2AAdapter a2aAdapter;

    public A2AGovernanceController(A2AAdapter a2aAdapter) {
        this.a2aAdapter = a2aAdapter;
    }

    @PostMapping("/rpc")
    public RPCResponse handleRPC(@RequestBody RPCRequest request) {
        return a2aAdapter.dispatch(request);
    }
}
