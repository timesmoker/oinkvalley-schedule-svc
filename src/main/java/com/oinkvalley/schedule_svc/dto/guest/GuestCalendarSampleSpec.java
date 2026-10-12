package com.oinkvalley.schedule_svc.dto.guest;

import java.util.List;

/**
 * Helm {@code guestCalendar.bootstrap.samples} 한 항목.
 * {@code tags} — 게스트 본인 소유 태그 이름(없으면 생성). 비어 있으면 ETC.
 */
public record GuestCalendarSampleSpec(
        int dayOffset,
        String title,
        String description,
        int startHour,
        int endHour,
        List<String> tags
) {
}
