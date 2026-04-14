package com.coagent4u.persistence.coordination;

import java.time.LocalDate;

/**
 * Projection for daily activity statistics.
 */
public interface ActivityStatsProjection {
    LocalDate getDay();
    int getCompleted();
    int getRejected();
    int getFailed();
}
