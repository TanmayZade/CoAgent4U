package com.coagent4u.persistence.coordination;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "coordination_state_log")
public class StateLogJpaEntity {

    @Id
    @Column(name = "log_id")
    private UUID logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordination_id", nullable = false)
    private CoordinationJpaEntity coordination;

    @Column(name = "from_state", nullable = false, length = 32)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 32)
    private String toState;

    @Column(name = "reason")
    private String reason;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "trigger_source", length = 64)
    private String triggerSource;

    @Column(name = "transitioned_at", nullable = false, updatable = false)
    private Instant transitionedAt;

    protected StateLogJpaEntity() {
    }

    public StateLogJpaEntity(UUID logId, String fromState, String toState, String reason, Instant transitionedAt) {
        this.logId = logId;
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.transitionedAt = transitionedAt;
    }

    public UUID getLogId() {
        return logId;
    }

    public void setLogId(UUID id) {
        this.logId = id;
    }

    public CoordinationJpaEntity getCoordination() {
        return coordination;
    }

    public void setCoordination(CoordinationJpaEntity c) {
        this.coordination = c;
    }

    public String getFromState() {
        return fromState;
    }

    public void setFromState(String s) {
        this.fromState = s;
    }

    public String getToState() {
        return toState;
    }

    public void setToState(String s) {
        this.toState = s;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String r) {
        this.reason = r;
    }

    public Instant getTransitionedAt() {
        return transitionedAt;
    }

    public void setTransitionedAt(Instant t) {
        this.transitionedAt = t;
    }
}
