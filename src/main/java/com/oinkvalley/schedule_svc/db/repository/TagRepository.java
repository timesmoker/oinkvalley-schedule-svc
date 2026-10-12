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

	/** `updated_at`은 건드리지 않고 `last_used_at`만 갱신 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Tag t SET t.lastUsedAt = :usedAt WHERE t.id = :id")
	void updateLastUsedAt(@Param("id") Long id, @Param("usedAt") Instant usedAt);
}
