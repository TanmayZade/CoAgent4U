package com.coagent4u.agent.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves natural-language date/time expressions into concrete {@link Instant}
 * values.
 * <p>
 * Handles patterns like:
 * <ul>
 * <li>"next Monday at 10 am"</li>
 * <li>"tomorrow at 3:30 pm"</li>
 * <li>"today at 2 pm"</li>
 * <li>"March 15 at 9 am"</li>
 * <li>"10 am" (defaults to today/tomorrow)</li>
 * </ul>
 */
public class NaturalDateResolver {

    private static final Logger log = LoggerFactory.getLogger(NaturalDateResolver.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy 'at' hh:mm a");

    // Day names
    private static final Pattern DAY_PATTERN = Pattern.compile(
            "(?:next\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)",
            Pattern.CASE_INSENSITIVE);

    // Time patterns: "10 am", "10:30 am", "3pm", "15:00"
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
            Pattern.CASE_INSENSITIVE);

    // "today" / "tomorrow"
    private static final Pattern RELATIVE_DAY = Pattern.compile(
            "\\b(today|tomorrow)\\b",
            Pattern.CASE_INSENSITIVE);

    // Month-day: "March 15", "Mar 10"
    private static final Pattern MONTH_DAY = Pattern.compile(
            "(?i)(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(\\d{1,2})");

    /**
     * Resolves a natural-language date/time string to an Instant in IST.
     *
     * @param dateTimeText e.g. "next Monday at 10 am"
     * @return the resolved Instant, or empty if unparseable
     */
    public Optional<Instant> resolve(String dateTimeText) {
        if (dateTimeText == null || dateTimeText.isBlank()) {
            log.warn("[DateResolver] Empty dateTime text");
            return Optional.empty();
        }

        String text = dateTimeText.toLowerCase(Locale.ENGLISH).trim();
        log.info("[DateResolver] Resolving: \"{}\"", dateTimeText);

        // Resolve date
        LocalDate date = resolveDate(text);

        // Resolve time
        LocalTime time = resolveTime(text);

        if (date == null && time == null) {
            log.warn("[DateResolver] Could not parse any date or time from: \"{}\"", dateTimeText);
            return Optional.empty();
        }

        if (date == null) {
            date = LocalDate.now(IST);
            if (time.isBefore(LocalTime.now(IST))) {
                date = date.plusDays(1);
            }
        }

        if (time == null) {
            time = LocalTime.of(10, 0);
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        Instant result = dateTime.atZone(IST).toInstant();

        log.info("[DateResolver] Resolved \"{}\" → {}", dateTimeText, format(result));
        return Optional.of(result);
    }

    /**
     * Formats an Instant as a human-readable IST date/time string.
     * Example: "Mon, 09 Mar 2026 at 10:00 AM"
     */
    public String format(Instant instant) {
        ZonedDateTime zdt = instant.atZone(IST);
        return zdt.format(DISPLAY_FORMAT);
    }

    /**
     * Formats a time range: "Mon, 09 Mar 2026, 10:00 AM – 11:00 AM"
     */
    public String formatRange(Instant start, Instant end) {
        ZonedDateTime startZdt = start.atZone(IST);
        ZonedDateTime endZdt = end.atZone(IST);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        return startZdt.format(dateFmt) + "\n"
                + startZdt.format(timeFmt) + " – " + endZdt.format(timeFmt);
    }

    private LocalDate resolveDate(String text) {
        // Check "today" / "tomorrow"
        Matcher relMatcher = RELATIVE_DAY.matcher(text);
        if (relMatcher.find()) {
            LocalDate date = "tomorrow".equals(relMatcher.group(1))
                    ? LocalDate.now(IST).plusDays(1)
                    : LocalDate.now(IST);
            log.info("[DateResolver] Resolved relative day: \"{}\" → {}", relMatcher.group(1), date);
            return date;
        }

        // Check day of week ("next Monday", "Monday")
        Matcher dayMatcher = DAY_PATTERN.matcher(text);
        if (dayMatcher.find()) {
            DayOfWeek target = parseDayOfWeek(dayMatcher.group(1));
            LocalDate today = LocalDate.now(IST);
            LocalDate next = today.with(TemporalAdjusters.next(target));
            log.info("[DateResolver] Resolved day: \"{}\" → {}", dayMatcher.group(), next);
            return next;
        }

        // Check month-day ("March 15")
        Matcher mdMatcher = MONTH_DAY.matcher(text);
        if (mdMatcher.find()) {
            int month = parseMonth(mdMatcher.group(1));
            int day = Integer.parseInt(mdMatcher.group(2));
            int year = LocalDate.now(IST).getYear();
            LocalDate result = LocalDate.of(year, month, day);
            if (result.isBefore(LocalDate.now(IST))) {
                result = result.plusYears(1);
            }
            log.info("[DateResolver] Resolved month-day: \"{} {}\" → {}", mdMatcher.group(1), day, result);
            return result;
        }

        return null;
    }

    private LocalTime resolveTime(String text) {
        // Normalize dot-separated variants: "p.m." → "pm", "a.m." → "am"
        text = text.replaceAll("(?i)p\\.m\\.?", "pm").replaceAll("(?i)a\\.m\\.?", "am");

        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = timeMatcher.group(2) != null ? Integer.parseInt(timeMatcher.group(2)) : 0;
            String ampm = timeMatcher.group(3);

            if (ampm != null) {
                if ("pm".equalsIgnoreCase(ampm) && hour < 12)
                    hour += 12;
                if ("am".equalsIgnoreCase(ampm) && hour == 12)
                    hour = 0;
            }

            LocalTime time = LocalTime.of(hour, minute);
            log.info("[DateResolver] Resolved time: {}", time);
            return time;
        }
        return null;
    }

    private DayOfWeek parseDayOfWeek(String name) {
        return switch (name.toLowerCase()) {
            case "monday" -> DayOfWeek.MONDAY;
            case "tuesday" -> DayOfWeek.TUESDAY;
            case "wednesday" -> DayOfWeek.WEDNESDAY;
            case "thursday" -> DayOfWeek.THURSDAY;
            case "friday" -> DayOfWeek.FRIDAY;
            case "saturday" -> DayOfWeek.SATURDAY;
            case "sunday" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Unknown day: " + name);
        };
    }

    private int parseMonth(String name) {
        return switch (name.toLowerCase().substring(0, 3)) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> throw new IllegalArgumentException("Unknown month: " + name);
        };
    }
}
