package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.config.CalendarProperties;
import com.oinkvalley.schedule_svc.db.domain.UserHiddenTag;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import com.oinkvalley.schedule_svc.db.repository.UserHiddenTagRepository;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 사용자별 캘린더 숨김 태그. */
@Service
@RequiredArgsConstructor
@Transactional
public class UserHiddenTagService {

    private final UserHiddenTagRepository userHiddenTagRepository;
    private final TagRepository tagRepository;
    private final ScheduleAccessService scheduleAccessService;
    private final CalendarProperties calendarProperties;

    @Transactional(Transactional.TxType.SUPPORTS)
    public Set<Long> hiddenTagIdsForUser(Long userId) {
        return new HashSet<>(userHiddenTagRepository.findTagIdsByUserId(userId));
    }

    public void replaceHiddenTags(Long userId, List<Long> tagIds) {
        userHiddenTagRepository.deleteByUserId(userId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> defaultIds = calendarProperties.defaultVisibleTagIds();
        for (Long tagId : tagIds.stream().distinct().toList()) {
            if (tagId == null || tagId <= 0) {
                continue;
            }
            if (!tagRepository.existsById(tagId)) {
                continue;
            }
            if (!scheduleAccessService.canAccessTag(tagId, userId, defaultIds)) {
                continue;
            }
            userHiddenTagRepository.save(UserHiddenTag.builder()
                    .userId(userId)
                    .tagId(tagId)
                    .build());
        }
    }

    public void setTagHidden(Long userId, long tagId, boolean hidden) {
        List<Long> current = userHiddenTagRepository.findTagIdsByUserId(userId);
        Set<Long> next = new HashSet<>(current);
        if (hidden) {
            next.add(tagId);
        } else {
            next.remove(tagId);
        }
        replaceHiddenTags(userId, List.copyOf(next));
    }
}
