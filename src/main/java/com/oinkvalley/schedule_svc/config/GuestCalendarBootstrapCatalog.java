package com.oinkvalley.schedule_svc.config;

import com.oinkvalley.schedule_svc.dto.guest.GuestCalendarSampleSpec;
import com.oinkvalley.schedule_svc.dto.guest.GuestCalendarSubscribeSpec;
import com.oinkvalley.schedule_svc.service.CalendarTimeUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/** Helm {@code guestCalendar.bootstrap} → env JSON 파싱. */
@Component
public class GuestCalendarBootstrapCatalog {

    private static final ZoneId ZONE = CalendarTimeUtil.ZONE;

    private final String samplesJson;
    private final String subscribeTagsJson;
    private List<GuestCalendarSampleSpec> samples = List.of();
    private List<GuestCalendarSubscribeSpec> subscribeTags = List.of();

    public GuestCalendarBootstrapCatalog(
            @Value("${GUEST_CALENDAR_SAMPLES:[]}") String samplesJson,
            @Value("${GUEST_CALENDAR_SUBSCRIBE_TAGS:[]}") String subscribeTagsJson
    ) {
        this.samplesJson = samplesJson;
        this.subscribeTagsJson = subscribeTagsJson;
    }

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.shared();
        samples = parseList(mapper, samplesJson, new TypeReference<>() {});
        subscribeTags = parseList(mapper, subscribeTagsJson, new TypeReference<>() {});
    }

    public List<GuestCalendarSubscribeSpec> subscribeTags() {
        return subscribeTags;
    }

    public List<ResolvedGuestCalendarSample> resolveSamples(LocalDate today) {
        return samples.stream()
                .map(spec -> toResolved(today, spec))
                .toList();
    }

    private static <T> List<T> parseList(JsonMapper mapper, String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return mapper.readValue(json, type);
    }

    private static ResolvedGuestCalendarSample toResolved(LocalDate today, GuestCalendarSampleSpec spec) {
        LocalDate date = today.plusDays(spec.dayOffset());
        Instant start = date.atTime(LocalTime.of(spec.startHour(), 0)).atZone(ZONE).toInstant();
        Instant end = date.atTime(LocalTime.of(spec.endHour(), 0)).atZone(ZONE).toInstant();
        List<String> tags = spec.tags() == null ? List.of() : spec.tags();
        return new ResolvedGuestCalendarSample(spec.title(), spec.description(), start, end, tags);
    }

    public record ResolvedGuestCalendarSample(
            String title,
            String description,
            Instant start,
            Instant end,
            List<String> tagNames
    ) {
    }
}
