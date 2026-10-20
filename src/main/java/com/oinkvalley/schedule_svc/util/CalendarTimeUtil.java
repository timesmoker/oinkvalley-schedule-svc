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

    /** 종일 = 시작·끝이 모두 {@link #ZONE} 0시 (끝은 다음 날 0시, 배타). 24시간 넘는 시간 일정은 종일이 아님 */
    public static boolean isAllDaySpan(Instant start, Instant end) {
        return end.isAfter(start) && isMidnight(start) && isMidnight(end);
    }

    private static boolean isMidnight(Instant instant) {
        return instant.atZone(ZONE).toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}
