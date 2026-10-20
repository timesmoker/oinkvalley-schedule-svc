package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.CalendarUser;
import com.oinkvalley.schedule_svc.db.domain.Schedule;
import com.oinkvalley.schedule_svc.db.domain.ScheduleTag;
import com.oinkvalley.schedule_svc.db.domain.Tag;
import com.oinkvalley.schedule_svc.db.domain.TagMember;
import com.oinkvalley.schedule_svc.db.domain.TagType;
import com.oinkvalley.schedule_svc.client.UserProfileClient;
import com.oinkvalley.schedule_svc.db.domain.Visibility;
import com.oinkvalley.schedule_svc.db.repository.CalendarUserRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleTagRepository;
import com.oinkvalley.schedule_svc.db.repository.TagMemberRepository;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import com.oinkvalley.schedule_svc.dto.tag.CreateTagRequest;
import com.oinkvalley.schedule_svc.dto.tag.DiscoverTagResponse;
import com.oinkvalley.schedule_svc.dto.tag.TagListResponse;
import com.oinkvalley.schedule_svc.dto.tag.TagResponse;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 태그 목록·생성·구독·숨김(tag_members) 및 소유자 ETC 태그 보장. 소유자 구독 행은 DB 트리거가 만든다. */
@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private static final String ETC_NAME = "기타";

    private static final List<Visibility> DISCOVER_VISIBILITIES = List.of(Visibility.SHARED, Visibility.PUBLIC);

    /** 최근 사용(`last_used_at` DESC) → 미사용은 `created_at` DESC */
    private static final Comparator<Tag> TAG_DISPLAY_ORDER = Comparator
            .comparing(Tag::getLastUsedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Tag::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

    private final TagRepository tagRepository;
    private final CalendarUserRepository calendarUserRepository;
    private final UserProfileClient userProfileClient;
    private final TagMemberRepository tagMemberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleTagRepository scheduleTagRepository;

    @Transactional(Transactional.TxType.SUPPORTS)
    public TagListResponse listForUser(Long userId) {
        Set<Long> hiddenIds = new HashSet<>(tagMemberRepository.findHiddenTagIdsByUserId(userId));

        List<TagResponse> tags = tagRepository.findListedForUser(userId).stream()
                .sorted(TAG_DISPLAY_ORDER)
                .map(t -> toResponse(t, hiddenIds.contains(t.getId())))
                .toList();

        return new TagListResponse(tags);
    }

    /** 이메일로 사용자의 공개·공유(`SHARED`/`PUBLIC`) USER 태그 검색 */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<DiscoverTagResponse> discoverByOwnerEmail(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        CalendarUser owner = calendarUserRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (owner == null) {
            return List.of();
        }
        String nickname = userProfileClient.nicknameForUser(owner.getId());
        final String ownerNickname =
                nickname != null && !nickname.isBlank()
                        ? nickname
                        : owner.getEmail().contains("@")
                                ? owner.getEmail().substring(0, owner.getEmail().indexOf('@'))
                                : owner.getEmail();

        return tagRepository.findByOwnerIdAndVisibilityIn(owner.getId(), DISCOVER_VISIBILITIES).stream()
                .filter(t -> t.getType() == TagType.USER)
                .sorted(TAG_DISPLAY_ORDER)
                .map(t -> new DiscoverTagResponse(
                        t.getId(),
                        t.getName(),
                        t.getVisibility().name(),
                        owner.getId(),
                        owner.getEmail(),
                        ownerNickname
                ))
                .toList();
    }

    /** 스케줄에 태그가 연결될 때 호출 — `updated_at`은 변경하지 않음 */
    public void markTagsUsed(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            tagRepository.updateLastUsedAt(tagId, now);
        }
    }

    public TagResponse create(Long userId, CreateTagRequest request) {
        Visibility visibility = parseVisibility(request.visibility(), Visibility.PRIVATE);
        if (visibility == Visibility.PUBLIC) {
            visibility = Visibility.SHARED;
        }
        Tag tag = Tag.builder()
                .ownerId(userId)
                .name(request.name().trim())
                .type(TagType.USER)
                .visibility(visibility)
                .build();
        tag = tagRepository.save(tag);
        return toResponse(tag, false);
    }

    /** 숨김 목록 통째 교체 — 구독 중인 태그에만 적용 */
    public void setHiddenTags(Long userId, List<Long> tagIds) {
        List<Long> ids = tagIds == null ? List.of() : tagIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            tagMemberRepository.showAll(userId);
        } else {
            tagMemberRepository.replaceHidden(userId, ids);
        }
    }

    public TagResponse updateVisibility(Long userId, long tagId, String rawVisibility) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tag owner can change visibility");
        }
        if (tag.getType() == TagType.ETC) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ETC tag visibility cannot be changed");
        }
        Visibility visibility = parseVisibility(rawVisibility, tag.getVisibility());
        if (visibility == Visibility.PUBLIC) {
            visibility = Visibility.SHARED;
        }
        tag.updateVisibility(visibility);
        boolean hidden = isHidden(tagId, userId);
        return toResponse(tag, hidden);
    }

    /**
     * 소유 USER 태그 영구 삭제. 연결 일정에서 해당 태그만 제거하고, 남은 태그가 없으면 일정 소유자 ETC로 연결.
     */
    public void deleteOwnedTag(Long userId, long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tag owner can delete this tag");
        }
        if (tag.getType() == TagType.ETC) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ETC tag cannot be deleted");
        }

        for (ScheduleTag link : scheduleTagRepository.findByTagId(tagId)) {
            long scheduleId = link.getScheduleId();
            scheduleTagRepository.deleteByScheduleIdAndTagId(scheduleId, tagId);
            List<Long> remaining = scheduleTagRepository.findTagIdsByScheduleId(scheduleId);
            if (remaining.isEmpty()) {
                Schedule schedule = scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Schedule not found: " + scheduleId));
                long etcId = ensureEtcTag(schedule.getOwnerId()).getId();
                scheduleTagRepository.save(ScheduleTag.builder()
                        .scheduleId(scheduleId)
                        .tagId(etcId)
                        .build());
                markTagsUsed(List.of(etcId));
            }
        }

        tagRepository.delete(tag);
    }

    /** 구독 중인 태그 숨김/표시. 구독하지 않은 태그는 404 */
    public void setTagHidden(Long userId, long tagId, boolean hidden) {
        if (tagMemberRepository.updateHidden(tagId, userId, hidden) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not subscribed: " + tagId);
        }
    }

    public TagResponse rename(Long userId, long tagId, String rawName) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tag owner can rename this tag");
        }
        if (tag.getType() == TagType.ETC) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ETC tag cannot be renamed");
        }
        String name = rawName.trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name is required");
        }
        tag.updateName(name);
        boolean hidden = isHidden(tagId, userId);
        return toResponse(tag, hidden);
    }

    /** SHARED/PUBLIC 태그 구독 — `tag_members` 등록(이미 있으면 숨김 해제) */
    public TagResponse followTag(Long userId, long tagId) {
        Tag tag = requireFollowableTag(tagId);
        ensureTagMember(tagId, userId);
        tagMemberRepository.updateHidden(tagId, userId, false);
        return toResponse(tag, false);
    }

    /** 가져온 태그 구독 해제 — `tag_members` 행 삭제 */
    public void unfollowTag(Long userId, long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() == userId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot unfollow your own tag");
        }
        tagMemberRepository.deleteByTagIdAndUserId(tagId, userId);
    }

    public TagResponse addMember(Long ownerId, long tagId, String memberEmail) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() != ownerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tag owner can add members");
        }
        if (tag.getType() == TagType.ETC) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ETC tag cannot have members");
        }
        if (tag.getVisibility() == Visibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private tags cannot have members");
        }
        CalendarUser member = calendarUserRepository.findByEmailIgnoreCase(memberEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (member.getId() == ownerId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is already the tag owner");
        }
        ensureTagMember(tagId, member.getId());
        boolean hidden = isHidden(tagId, ownerId);
        return toResponse(tag, hidden);
    }

    public void removeMember(Long ownerId, long tagId, long memberUserId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getOwnerId() != ownerId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tag owner can remove members");
        }
        if (memberUserId == ownerId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot be removed from own tag");
        }
        tagMemberRepository.deleteByTagIdAndUserId(tagId, memberUserId);
    }

    public Tag ensureEtcTag(Long userId) {
        return tagRepository.findByOwnerIdAndType(userId, TagType.ETC)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .ownerId(userId)
                        .name(ETC_NAME)
                        .type(TagType.ETC)
                        .visibility(Visibility.PRIVATE)
                        .build()));
    }

    /**
     * 게스트 부트스트랩: 지정 소유자의 태그 구독({@code tag_members}).
     * 태그 없음·소유자 불일치·PRIVATE 는 무시.
     */
    public void followOwnerTagIfAllowed(Long userId, long ownerUserId, long tagId) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag == null || tag.getOwnerId() != ownerUserId) {
            return;
        }
        try {
            followTag(userId, tagId);
        } catch (ResponseStatusException ignored) {
            // 비공개·미존재 등 — 부트스트랩은 조용히 스킵
        }
    }

    /**
     * 게스트 부트스트랩: 이름 기준 본인 소유 USER/PRIVATE 태그 보장.
     * 없으면 PRIVATE USER 태그 생성.
     */
    public Tag ensureOwnedUserTagByName(Long userId, String rawName) {
        if (userId == null || rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name is required");
        }
        String name = rawName.trim();
        return tagRepository.findByOwnerIdAndName(userId, name)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .ownerId(userId)
                        .name(name)
                        .type(TagType.USER)
                        .visibility(Visibility.PRIVATE)
                        .build()));
    }

    private boolean isHidden(long tagId, long userId) {
        return tagMemberRepository.findHidden(tagId, userId).orElse(false);
    }

    private TagResponse toResponse(Tag tag, boolean hidden) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getType().name(),
                tag.getVisibility().name(),
                tag.getOwnerId(),
                hidden
        );
    }

    private Tag requireFollowableTag(long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tag.getVisibility() == Visibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Private tags cannot be followed");
        }
        return tag;
    }

    private void ensureTagMember(long tagId, long userId) {
        if (!tagMemberRepository.existsByTagIdAndUserId(tagId, userId)) {
            tagMemberRepository.save(TagMember.builder()
                    .tagId(tagId)
                    .userId(userId)
                    .build());
        }
    }

    private static Visibility parseVisibility(String raw, Visibility fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String v = raw.trim().toUpperCase();
        if ("PUBLIC".equals(v) || "SHARED".equals(v)) {
            return Visibility.valueOf(v.equals("PUBLIC") ? "PUBLIC" : "SHARED");
        }
        if ("PRIVATE".equals(v)) {
            return Visibility.PRIVATE;
        }
        return fallback;
    }
}
