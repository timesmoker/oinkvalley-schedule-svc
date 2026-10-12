package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.CalendarUser;
import com.oinkvalley.schedule_svc.db.domain.ScheduleParticipant;
import com.oinkvalley.schedule_svc.db.repository.CalendarUserRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleParticipantRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 참여자 이메일 ↔ userId 및 schedule_participants 동기화. */
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantService {

    private final CalendarUserRepository calendarUserRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public List<Long> resolveParticipantIds(List<String> rawEmails, long scheduleOwnerId) {
        if (rawEmails == null || rawEmails.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawEmails) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            normalized.add(raw.trim().toLowerCase());
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<CalendarUser> users = calendarUserRepository.findByEmailInIgnoreCase(normalized);
        List<Long> ids = new ArrayList<>();
        for (CalendarUser user : users) {
            if (user.getId() != scheduleOwnerId) {
                ids.add(user.getId());
            }
        }
        return ids.stream().distinct().toList();
    }

    public void replaceParticipants(long scheduleId, List<Long> participantIds) {
        scheduleParticipantRepository.deleteByScheduleId(scheduleId);
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }
        for (Long uid : participantIds) {
            if (uid == null || uid <= 0) {
                continue;
            }
            scheduleParticipantRepository.save(ScheduleParticipant.builder()
                    .scheduleId(scheduleId)
                    .userId(uid)
                    .build());
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<String> emailsForUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return calendarUserRepository.findAllById(userIds).stream()
                .map(CalendarUser::getEmail)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<Long, String> emailByUserId(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return calendarUserRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(CalendarUser::getId, CalendarUser::getEmail));
    }
}
