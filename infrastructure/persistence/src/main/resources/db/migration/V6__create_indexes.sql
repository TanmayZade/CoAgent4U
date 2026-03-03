-- V6: Performance indexes across all module tables

-- user-module indexes
CREATE INDEX idx_slack_identities_user_id ON slack_identities(linked_user_id);
CREATE INDEX idx_service_connections_user_id ON service_connections(user_id);
CREATE INDEX idx_service_connections_status ON service_connections(user_id, service_type, status);

-- agent-module indexes
CREATE INDEX idx_agents_user_id ON agents(user_id);

-- coordination-module indexes
CREATE INDEX idx_coordinations_requester ON coordinations(requester_agent_id);
CREATE INDEX idx_coordinations_invitee ON coordinations(invitee_agent_id);
CREATE INDEX idx_coordinations_state ON coordinations(state);
CREATE INDEX idx_coordination_state_log_coordination ON coordination_state_log(coordination_id);
CREATE INDEX idx_coordination_state_log_time ON coordination_state_log(transitioned_at);

-- approval-module indexes
CREATE INDEX idx_approvals_user_id ON approvals(user_id);
CREATE INDEX idx_approvals_coordination ON approvals(coordination_id);
CREATE INDEX idx_approvals_expires_at ON approvals(expires_at) WHERE decision = 'PENDING';
CREATE INDEX idx_approvals_pending ON approvals(user_id, decision) WHERE decision = 'PENDING';

-- audit indexes
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs(occurred_at);
