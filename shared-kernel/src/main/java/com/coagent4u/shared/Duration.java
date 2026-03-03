package com.coagent4u.shared;

/**
 * Meeting duration in minutes.
 * Used for scheduling constraints and availability matching.
 */
public record Duration(int minutes) {
    public Duration {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0 minutes, got: " + minutes);
        }
    }

    public static Duration of(int minutes) {
        return new Duration(minutes);
    }

    public static Duration ofHours(int hours) {
        return new Duration(hours * 60);
    }

    public java.time.Duration toJavaDuration() {
        return java.time.Duration.ofMinutes(minutes);
    }

    @Override
    public String toString() {
        return minutes + "min";
    }
}
