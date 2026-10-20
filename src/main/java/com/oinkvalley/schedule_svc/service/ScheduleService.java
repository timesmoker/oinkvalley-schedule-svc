package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.Schedule;
import com.oinkvalley.schedule_svc.db.domain.ScheduleTag;
import com.oinkvalley.schedule_svc.db.domain.Tag;
import com.oinkvalley.schedule_svc.db.domain.Visibility;
import com.oinkvalley.schedule_svc.db.repository.ScheduleParticipantRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleTagRepository;
import com.oinkvalley.schedule_svc.db.repository.TagMemberRepository;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import com.oinkvalley.schedule_svc.dto.schedule.CreateScheduleRequest;
import com.oinkvalley.schedule_svc.dto.schedule.ScheduleResponse;
import com.oinkvalley.schedule_svc.dto.schedule.UpdateScheduleRequest;
import com.oinkvalley.schedule_svc.util.CalendarTimeUtil;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 일정 조회·생성·수정·삭제. */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleTagRepository scheduleTagRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final TagRepository tagRepository;
    private final TagMemberRepository tagMemberRepository;
    private final ScheduleTagLinkService scheduleTagLinkService;
    private final ParticipantService participantService;

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ScheduleResponse> listVisible(
            Long userId,
            String from,
            String to,
            List<Long> filterTagIds,
            String scope
    ) {
        Instant rangeStart = CalendarTimeUtil.rangeStart(from);
        Instant rangeEnd = CalendarTimeUtil.rangeEndExclusive(to);
        Set<Long> filter = filterTagIds == null ? Set.of() : new HashSet<>(filterTagIds);
        boolean mineOnly = "mine".equalsIgnoreCase(scope);

        if (mineOnly && userId == null) {
            return List.of();
        }

        List<Long> visibleIds = userId == null
                ? scheduleRepository.findPublicIds(rangeStart, rangeEnd)
                : scheduleRepository.findVisibleIds(userId, rangeStart, rangeEnd);
        if (visibleIds.isEmpty()) {
            return List.of();
        }

        return buildResponses(userId, visibleIds, schedule -> {
            if (mineOnly && !schedule.isMine(userId)) {
                return false;
            }
            return filter.isEmpty() || schedule.tagIds().stream().anyMatch(filter::contains);
        });
    }

    public ScheduleResponse create(Long userId, CreateScheduleRequest request) {
        Schedule schedule = saveNewSchedule(userId, request);
        List<Long> tagIds = scheduleTagLinkService.resolveScheduleTagIds(
                userId, userId, request.tags());
        scheduleTagLinkService.replaceScheduleTags(schedule.getId(), tagIds);
        List<Long> linkedTagIds = scheduleTagRepository.findTagIdsByScheduleId(schedule.getId());

        List<Long> participantIds = participantService.resolveParticipantIds(
                request.participantEmails(), userId);
        participantService.replaceParticipants(schedule.getId(), participantIds);

        return toResponse(schedule, linkedTagIds, participantIds);
    }

    public ScheduleResponse update(Long userId, long scheduleId, UpdateScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found: " + scheduleId));
        if (!schedule.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the schedule owner can update this schedule");
        }

        Instant start = CalendarTimeUtil.toStartInstant(
                request.startDate(), request.allDay(), request.startTime());
        Instant end = CalendarTimeUtil.toEndInstant(
                request.endDate(), request.allDay(), request.endTime());
        if (!end.isAfter(start)) {
            end = start.plusSeconds(request.allDay() ? 24 * 3600 : 3600);
        }

        schedule.updateDetails(
                request.title().trim(),
                blankToNull(request.note()),
                blankToNull(request.link()),
                parseScheduleVisibility(request.visibility()),
                start,
                end
        );

        List<Long> tagIds = scheduleTagLinkService.resolveScheduleTagIds(
                userId, schedule.getOwnerId(), request.tags());
        scheduleTagLinkService.replaceScheduleTags(schedule.getId(), tagIds);
        List<Long> linkedTagIds = scheduleTagRepository.findTagIdsByScheduleId(schedule.getId());

        List<Long> participantIds = participantService.resolveParticipantIds(
                request.participantEmails(), schedule.getOwnerId());
        participantService.replaceParticipants(schedule.getId(), participantIds);

        return toResponse(schedule, linkedTagIds, participantIds);
    }

    public void delete(Long userId, long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found: " + scheduleId));
        if (!schedule.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the schedule owner can delete this schedule");
        }
        scheduleRepository.delete(schedule);
    }

    private Schedule saveNewSchedule(Long userId, CreateScheduleRequest request) {
        Instant start = CalendarTimeUtil.toStartInstant(
                request.startDate(), request.allDay(), request.startTime());
        Instant end = CalendarTimeUtil.toEndInstant(
                request.endDate(), request.allDay(), request.endTime());
        if (!end.isAfter(start)) {
            end = start.plusSeconds(request.allDay() ? 24 * 3600 : 3600);
        }

        Schedule schedule = Schedule.builder()
                .ownerId(userId)
                .title(request.title().trim())
                .description(blankToNull(request.note()))
                .link(blankToNull(request.link()))
                .visibility(parseScheduleVisibility(request.visibility()))
                .startTime(start)
                .endTime(end)
                .build();
        return scheduleRepository.save(schedule);
    }

    /** 응답 조립 전 일정·태그·참여자를 묶어 둔 것 — scope·태그 필터를 추가 쿼리 없이 적용 */
    private record LoadedSchedule(Schedule schedule, List<Long> tagIds, List<Long> participantIds) {
        boolean isMine(Long userId) {
            return schedule.getOwnerId().equals(userId) || participantIds.contains(userId);
        }
    }

    private List<ScheduleResponse> buildResponses(
            Long viewerId,
            List<Long> visibleIdList,
            Predicate<LoadedSchedule> include
    ) {
        List<Schedule> schedules = scheduleRepository.findAllById(visibleIdList);
        var scheduleTags = scheduleTagRepository.findByScheduleIdIn(visibleIdList);
        var participants = scheduleParticipantRepository.findByScheduleIdIn(visibleIdList);
        Set<Long> viewableTagIds = viewableTagIds(viewerId, scheduleTags);
        Map<Long, List<Long>> tagsBySchedule = ScheduleMapper.tagIdsBySchedule(
                scheduleTags.stream().filter(link -> viewableTagIds.contains(link.getTagId())).toList());
        Map<Long, List<Long>> participantsBySchedule = ScheduleMapper.participantIdsBySchedule(participants);

        List<LoadedSchedule> loaded = schedules.stream()
                .map(e -> new LoadedSchedule(
                        e,
                        tagsBySchedule.getOrDefault(e.getId(), List.of()),
                        participantsBySchedule.getOrDefault(e.getId(), List.of())))
                .filter(include)
                .toList();

        Set<Long> allParticipantIds = new HashSet<>();
        for (LoadedSchedule l : loaded) {
            if (l.schedule().getOwnerId().equals(viewerId)) {
                allParticipantIds.addAll(l.participantIds());
            }
        }
        Map<Long, String> emailByUserId = participantService.emailByUserId(new ArrayList<>(allParticipantIds));

        return loaded.stream()
                .map(l -> {
                    // 참여자: 비로그인에게는 아무것도, 이메일은 일정 소유자(수정 화면)에게만
                    if (viewerId == null) {
                        return ScheduleMapper.toResponse(l.schedule(), l.tagIds(), List.of(), List.of());
                    }
                    List<String> emails = l.schedule().getOwnerId().equals(viewerId)
                            ? l.participantIds().stream()
                                    .map(id -> emailByUserId.getOrDefault(id, ""))
                                    .filter(s -> !s.isBlank())
                                    .toList()
                            : List.of();
                    return ScheduleMapper.toResponse(l.schedule(), l.tagIds(), l.participantIds(), emails);
                })
                .toList();
    }

    /**
     * 일정에 붙은 태그 중 보는 사람이 접근할 수 있는 것만 (남의 PRIVATE 태그·구독 안 한 SHARED 태그는 응답에서 뺀다).
     * 규칙은 {@link ScheduleAccessService#canAccessTag} 와 같다. 쿼리 2번(태그, 내 구독).
     */
    private Set<Long> viewableTagIds(Long viewerId, List<ScheduleTag> links) {
        Set<Long> tagIds = new HashSet<>();
        for (ScheduleTag link : links) {
            tagIds.add(link.getTagId());
        }
        if (tagIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> subscribed = viewerId == null
                ? Set.of()
                : new HashSet<>(tagMemberRepository.findTagIdsByUserId(viewerId));
        Set<Long> viewable = new HashSet<>();
        for (Tag tag : tagRepository.findAllById(tagIds)) {
            boolean ok = tag.getVisibility() == Visibility.PUBLIC
                    || (viewerId != null && tag.getOwnerId() == viewerId.longValue())
                    || (tag.getVisibility() == Visibility.SHARED && subscribed.contains(tag.getId()));
            if (ok) {
                viewable.add(tag.getId());
            }
        }
        return viewable;
    }

    private ScheduleResponse toResponse(Schedule schedule, List<Long> tagIds, List<Long> participantIds) {
        List<String> emails = participantService.emailsForUserIds(participantIds);
        return ScheduleMapper.toResponse(schedule, tagIds, participantIds, emails);
    }

    private static Visibility parseScheduleVisibility(String raw) {
        if (raw == null || raw.isBlank()) {
            return Visibility.PRIVATE;
        }
        try {
            return Visibility.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility: " + raw);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
