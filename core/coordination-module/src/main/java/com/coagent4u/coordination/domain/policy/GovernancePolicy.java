package com.coagent4u.coordination.domain.policy;

import com.coagent4u.coordination.domain.Coordination;

/**
 * Domain interface for governance policies.
 */
public interface GovernancePolicy {
    
    /**
     * Evaluates a coordination session against the policy.
     * 
     * @param coordination The session to evaluate
     * @return PolicyResult containing the decision (ALLOW, REJECT, or REQUIRE_APPROVAL)
     */
    PolicyResult evaluate(Coordination coordination);
}
