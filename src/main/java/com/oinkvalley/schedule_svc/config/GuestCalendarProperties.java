package com.oinkvalley.schedule_svc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 게스트 샘플 일정 시드. enabled: GUEST_CALENDAR_BOOTSTRAP_ENABLED, samples: GUEST_CALENDAR_SAMPLES (Helm SOT) */
@Component
@ConfigurationProperties(prefix = "guest.calendar.bootstrap")
@Getter
@Setter
public class GuestCalendarProperties {

    private boolean enabled;
}
