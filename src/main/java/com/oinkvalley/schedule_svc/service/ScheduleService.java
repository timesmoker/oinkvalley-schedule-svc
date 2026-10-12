package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.config.CalendarProperties;
import com.oinkvalley.schedule_svc.db.domain.Schedule;
import com.oinkvalley.schedule_svc.db.domain.Visibility;
import com.oinkvalley.schedule_svc.db.repository.ScheduleParticipantRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleTagRepository;
import com.oinkvalley.schedule_svc.dto.schedule.CreateScheduleRequest;
import com.oinkvalley.schedule_svc.dto.schedule.ScheduleResponse;
import com.oinkvalley.schedule_svc.dto.schedule.UpdateScheduleRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ScheduleAccessService scheduleAccessService;
    private final ScheduleTagLinkService scheduleTagLinkService;
    private final ParticipantService participantService;
    private final CalendarProperties calendarProperties;

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
        List<Long> defaultTagIds = calendarProperties.defaultVisibleTagIds();
        Set<Long> filter = filterTagIds == null ? Set.of() : new HashSet<>(filterTagIds);
        boolean mineOnly = "mine".equalsIgnoreCase(scope);

        if (mineOnly && userId == null) {
            return List.of();
        }

        List<Schedule> candidates = scheduleRepository.findOverlapping(rangeStart, rangeEnd);
        Set<Long> visibleIds = new LinkedHashSet<>();
        for (Schedule schedule : candidates) {
            if (!scheduleAccessService.canView(schedule, userId, defaultTagIds)) {
                continue;
            }
            if (mineOnly && userId != null && !scheduleAccessService.isMine(schedule, userId)) {
                continue;
            }
            if (!filter.isEmpty()) {
                List<Long> tagIds = scheduleTagRepository.findTagIdsByScheduleId(schedule.getId());
                boolean matches = tagIds.stream().anyMatch(filter::contains);
                if (!matches) {
                    continue;
                }
            }
            visibleIds.add(schedule.getId());
        }
        if (visibleIds.isEmpty()) {
            return List.of();
        }

        return buildResponses(List.copyOf(visibleIds));
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

    private List<ScheduleResponse> buildResponses(List<Long> visibleIdList) {
        List<Schedule> schedules = scheduleRepository.findAllById(visibleIdList);
        var scheduleTags = scheduleTagRepository.findByScheduleIdIn(visibleIdList);
        var participants = scheduleParticipantRepository.findByScheduleIdIn(visibleIdList);
        Map<Long, List<Long>> tagsBySchedule = ScheduleMapper.tagIdsBySchedule(scheduleTags);
        Map<Long, List<Long>> participantsBySchedule = ScheduleMapper.participantIdsBySchedule(participants);

        Set<Long> allParticipantIds = new HashSet<>();
        for (List<Long> ids : participantsBySchedule.values()) {
            allParticipantIds.addAll(ids);
        }
        Map<Long, String> emailByUserId = participantService.emailByUserId(new ArrayList<>(allParticipantIds));

        return schedules.stream()
                .map(e -> {
                    List<Long> pids = participantsBySchedule.getOrDefault(e.getId(), List.of());
                    List<String> emails = pids.stream()
                            .map(id -> emailByUserId.getOrDefault(id, ""))
                            .filter(s -> !s.isBlank())
                            .toList();
                    return ScheduleMapper.toResponse(
                            e,
                            tagsBySchedule.getOrDefault(e.getId(), List.of()),
                            pids,
                            emails
                    );
                })
                .toList();
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
