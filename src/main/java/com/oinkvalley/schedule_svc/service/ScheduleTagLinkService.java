package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.ScheduleTag;
import com.oinkvalley.schedule_svc.db.domain.Tag;
import com.oinkvalley.schedule_svc.db.domain.TagType;
import com.oinkvalley.schedule_svc.db.repository.ScheduleTagRepository;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 일정–태그 연결 및 ETC 정규화(설계 시나리오 1–3). */
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleTagLinkService {

    private final ScheduleTagRepository scheduleTagRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final ScheduleAccessService scheduleAccessService;

    /**
     * 접근 가능한 태그만 파싱한 뒤 ETC 규칙 적용.
     * 일반 태그가 있으면 ETC 제거, 없으면 소유자 ETC 1개.
     */
    public List<Long> resolveScheduleTagIds(Long actorUserId, long scheduleOwnerId, List<String> rawTags) {
        long etcId = tagService.ensureEtcTag(scheduleOwnerId).getId();

        Set<Long> parsed = new HashSet<>();
        if (rawTags != null) {
            for (String raw : rawTags) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(raw.trim());
                    if (scheduleAccessService.canAccessTag(id, actorUserId)
                            || id == etcId) {
                        parsed.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // legacy slug — skip
                }
            }
        }

        if (parsed.isEmpty()) {
            return List.of(etcId);
        }

        boolean hasNonEtcUserTag = false;
        for (Long tagId : parsed) {
            if (tagId.equals(etcId)) {
                continue;
            }
            Tag tag = tagRepository.findById(tagId).orElse(null);
            if (tag != null && tag.getType() == TagType.USER) {
                hasNonEtcUserTag = true;
                break;
            }
        }

        if (!hasNonEtcUserTag) {
            return List.of(etcId);
        }

        List<Long> out = new ArrayList<>();
        for (Long tagId : parsed) {
            if (!tagId.equals(etcId)) {
                out.add(tagId);
            }
        }
        return out.stream().distinct().toList();
    }

    /**
     * 일정–태그 연결을 통째로 교체한다.
     * derived {@code deleteByScheduleId}는 flush 시점에 INSERT보다 늦게 실행될 수 있어
     * 방금 넣은 행이 지워지고 {@code tags.last_used_at}만 남는 현상이 있었다.
     */
    public void replaceScheduleTags(long scheduleId, List<Long> tagIds) {
        List<Long> expected = tagIds == null
                ? List.of()
                : tagIds.stream().filter(id -> id != null).distinct().toList();

        scheduleTagRepository.deleteAllByScheduleId(scheduleId);
        for (Long tagId : expected) {
            scheduleTagRepository.save(ScheduleTag.builder()
                    .scheduleId(scheduleId)
                    .tagId(tagId)
                    .build());
        }
        scheduleTagRepository.flush();

        List<Long> persisted = scheduleTagRepository.findTagIdsByScheduleId(scheduleId);
        if (!new HashSet<>(persisted).equals(new HashSet<>(expected))) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to persist schedule_tags for schedule " + scheduleId);
        }
        tagService.markTagsUsed(expected);
    }
}
