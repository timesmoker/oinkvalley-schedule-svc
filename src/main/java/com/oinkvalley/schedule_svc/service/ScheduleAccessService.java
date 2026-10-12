package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.Schedule;
import com.oinkvalley.schedule_svc.db.domain.Tag;
import com.oinkvalley.schedule_svc.db.domain.Visibility;
import com.oinkvalley.schedule_svc.db.repository.ScheduleParticipantRepository;
import com.oinkvalley.schedule_svc.db.repository.ScheduleTagRepository;
import com.oinkvalley.schedule_svc.db.repository.TagMemberRepository;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 일정·태그 visibility 및 공유 멤버 기준 조회 권한. */
@Service
@RequiredArgsConstructor
public class ScheduleAccessService {

	private final ScheduleTagRepository scheduleTagRepository;
	private final ScheduleParticipantRepository scheduleParticipantRepository;
	private final TagRepository tagRepository;
	private final TagMemberRepository tagMemberRepository;

	public boolean isMine(Schedule schedule, long userId) {
		if (schedule.getOwnerId() == userId) {
			return true;
		}
		return scheduleParticipantRepository.existsByScheduleIdAndUserId(schedule.getId(), userId);
	}

	public boolean canView(Schedule schedule, Long userId, Collection<Long> defaultVisibleTagIds) {
		if (userId == null) {
			return schedule.getVisibility() == Visibility.PUBLIC;
		}
		if (isMine(schedule, userId)) {
			return true;
		}
		if (schedule.getVisibility() == Visibility.PUBLIC) {
			return true;
		}

		List<Long> linkedTagIds = scheduleTagRepository.findTagIdsByScheduleId(schedule.getId());
		if (linkedTagIds.isEmpty()) {
			return false;
		}

		Set<Long> defaultTags = defaultVisibleTagIds == null
				? Set.of()
				: new HashSet<>(defaultVisibleTagIds);

		boolean onDefaultTag = linkedTagIds.stream().anyMatch(defaultTags::contains);
		if (onDefaultTag && (schedule.getVisibility() == Visibility.PUBLIC || schedule.getVisibility() == Visibility.SHARED)) {
			return true;
		}

		if (schedule.getVisibility() != Visibility.SHARED) {
			return false;
		}

		for (Long tagId : linkedTagIds) {
			if (canAccessTag(tagId, userId, defaultTags)) {
				return true;
			}
		}
		return false;
	}

	public boolean canAccessTag(long tagId, Long userId, Collection<Long> defaultVisibleTagIds) {
		if (userId == null) {
			return tagRepository.findById(tagId)
					.map(t -> t.getVisibility() == Visibility.PUBLIC)
					.orElse(false);
		}
		Set<Long> defaultTags = defaultVisibleTagIds == null
				? Set.of()
				: new HashSet<>(defaultVisibleTagIds);
		if (defaultTags.contains(tagId)) {
			return true;
		}
		return tagRepository.findById(tagId)
				.map(tag -> canAccessTagEntity(tag, userId))
				.orElse(false);
	}

	private boolean canAccessTagEntity(Tag tag, long userId) {
		if (tag.getOwnerId() == userId) {
			return true;
		}
		if (tag.getVisibility() == Visibility.PUBLIC) {
			return true;
		}
		if (tag.getVisibility() == Visibility.SHARED) {
			return tagMemberRepository.existsByTagIdAndUserId(tag.getId(), userId);
		}
		return false;
	}
}
