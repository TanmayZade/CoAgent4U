package com.coagent4u.app.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.coagent4u.approval.application.ApprovalService;

/**
 * Scheduled job that periodically scans for and expires overdue approval
 * requests.
 * Runs every 15 minutes. Approvals have a 12-hour timeout (per PRD §4.3).
 */
@Component
public class ApprovalExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ApprovalExpirationJob.class);

    private final ApprovalService approvalService;

    public ApprovalExpirationJob(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Scheduled(fixedRate = 15 * 60 * 1000) // every 15 minutes
    public void expireOverdueApprovals() {
        int expired = approvalService.expireOverdue();
        if (expired > 0) {
            log.info("Expired {} overdue approval(s)", expired);
        }
    }
}
