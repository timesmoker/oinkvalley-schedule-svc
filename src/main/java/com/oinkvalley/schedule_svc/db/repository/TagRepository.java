package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oinkvalley.schedule_svc.db.domain.Visibility;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

	List<Tag> findByOwnerId(Long ownerId);

	List<Tag> findByOwnerIdAndVisibilityIn(Long ownerId, Collection<Visibility> visibilities);

	Optional<Tag> findByOwnerIdAndType(Long ownerId, com.oinkvalley.schedule_svc.db.domain.TagType type);

	Optional<Tag> findByOwnerIdAndName(Long ownerId, String name);

	/** 사용자 태그 목록: 구독(tag_members) 중인 태그 — 내 태그 + PRIVATE 가 아닌 남의 태그. 숨긴 태그 포함 */
	@Query(nativeQuery = true, value = """
			SELECT t.* FROM tags t
			JOIN tag_members m ON m.tag_id = t.id
			WHERE m.user_id = :userId
			  AND (t.owner_id = :userId OR t.visibility <> 'PRIVATE')
			""")
	List<Tag> findListedForUser(@Param("userId") long userId);

	/**
	 * `updated_at`은 건드리지 않고 `last_used_at`만 갱신.
	 * flushAutomatically: 컨텍스트를 비우기 전에 대기 중인 INSERT(예: 태그 삭제 시 ETC 연결)를 먼저 반영한다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Tag t SET t.lastUsedAt = :usedAt WHERE t.id = :id")
	void updateLastUsedAt(@Param("id") Long id, @Param("usedAt") Instant usedAt);
}
