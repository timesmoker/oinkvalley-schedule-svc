package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.Tag;
import com.oinkvalley.schedule_svc.db.domain.Visibility;
import com.oinkvalley.schedule_svc.db.repository.TagMemberRepository;
import com.oinkvalley.schedule_svc.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 태그 visibility 및 공유 멤버 기준 태그 접근 권한. 일정 목록 권한은 ScheduleRepository.findVisibleIds. */
@Service
@RequiredArgsConstructor
public class ScheduleAccessService {

	private final TagRepository tagRepository;
	private final TagMemberRepository tagMemberRepository;

	/** 태그 접근: 소유자, PUBLIC, 또는 SHARED 이면서 구독(tag_members) 중 */
	public boolean canAccessTag(long tagId, Long userId) {
		return tagRepository.findById(tagId)
				.map(tag -> canAccessTagEntity(tag, userId))
				.orElse(false);
	}

	private boolean canAccessTagEntity(Tag tag, Long userId) {
		if (tag.getVisibility() == Visibility.PUBLIC) {
			return true;
		}
		if (userId == null) {
			return false;
		}
		if (tag.getOwnerId() == userId.longValue()) {
			return true;
		}
		if (tag.getVisibility() == Visibility.SHARED) {
			return tagMemberRepository.existsByTagIdAndUserId(tag.getId(), userId);
		}
		return false;
	}
}
