package com.oinkvalley.schedule_svc.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class CalendarTimeUtil {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private CalendarTimeUtil() {
    }

    public static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + raw);
        }
    }

    public static LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(raw.trim(), TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time: " + raw);
        }
    }

    public static Instant rangeStart(String fromDate) {
        return parseDate(fromDate).atStartOfDay(ZONE).toInstant();
    }

    public static Instant rangeEndExclusive(String toDate) {
        return parseDate(toDate).plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    public static Instant toStartInstant(String date, boolean allDay, String time) {
        LocalDate d = parseDate(date);
        if (allDay) {
            return d.atStartOfDay(ZONE).toInstant();
        }
        LocalTime t = time != null && !time.isBlank() ? parseTime(time) : LocalTime.MIDNIGHT;
        return d.atTime(t).atZone(ZONE).toInstant();
    }

    public static Instant toEndInstant(String date, boolean allDay, String time) {
        LocalDate d = parseDate(date);
        if (allDay) {
            return d.plusDays(1).atStartOfDay(ZONE).toInstant();
        }
        LocalTime t = time != null && !time.isBlank() ? parseTime(time) : LocalTime.of(23, 59);
        return d.atTime(t).atZone(ZONE).toInstant();
    }

    public static String formatDate(Instant instant) {
        return instant.atZone(ZONE).toLocalDate().format(DATE);
    }

    public static String formatTime(Instant instant) {
        return instant.atZone(ZONE).toLocalTime().format(TIME);
    }

    public static boolean isAllDaySpan(Instant start, Instant end) {
        long seconds = end.getEpochSecond() - start.getEpochSecond();
        return seconds >= 24 * 60 * 60 - 60;
    }
}
