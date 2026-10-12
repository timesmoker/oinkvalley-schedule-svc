package com.oinkvalley.schedule_svc.dto.guest;

import java.util.List;

/** Helm {@code guestCalendar.bootstrap.subscribeTags} 한 항목. */
public record GuestCalendarSubscribeSpec(
        long ownerUserId,
        List<Long> tagIds
) {
}
