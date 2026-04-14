package com.coagent4u.coordination.application.dto;

import java.time.LocalDate;

/**
 * Statistics for coordination activity on a specific day.
 */
public record CoordinationActivityPoint(
        LocalDate date,
        int completed,
        int rejected,
        int failed
) {}
