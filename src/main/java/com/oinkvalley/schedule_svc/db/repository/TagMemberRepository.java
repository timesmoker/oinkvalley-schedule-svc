package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.TagMember;
import com.oinkvalley.schedule_svc.db.domain.TagMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagMemberRepository extends JpaRepository<TagMember, TagMemberId> {

	boolean existsByTagIdAndUserId(Long tagId, Long userId);

	@Query("SELECT tm.tagId FROM TagMember tm WHERE tm.userId = :userId")
	List<Long> findTagIdsByUserId(@Param("userId") Long userId);

	void deleteByTagIdAndUserId(Long tagId, Long userId);

	@Query("SELECT tm.tagId FROM TagMember tm WHERE tm.userId = :userId AND tm.hidden = true")
	List<Long> findHiddenTagIdsByUserId(@Param("userId") Long userId);

	@Query("SELECT tm.hidden FROM TagMember tm WHERE tm.tagId = :tagId AND tm.userId = :userId")
	Optional<Boolean> findHidden(@Param("tagId") Long tagId, @Param("userId") Long userId);

	/** @return 바뀐 행 수 — 0 이면 구독하지 않은 태그 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE TagMember tm SET tm.hidden = :hidden WHERE tm.tagId = :tagId AND tm.userId = :userId")
	int updateHidden(@Param("tagId") Long tagId, @Param("userId") Long userId, @Param("hidden") boolean hidden);

	/** 숨김 목록 통째 교체: tagIds 에 있으면 숨김, 없으면 보임 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE TagMember tm
			SET tm.hidden = CASE WHEN tm.tagId IN :tagIds THEN true ELSE false END
			WHERE tm.userId = :userId
			""")
	void replaceHidden(@Param("userId") Long userId, @Param("tagIds") Collection<Long> tagIds);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE TagMember tm SET tm.hidden = false WHERE tm.userId = :userId")
	void showAll(@Param("userId") Long userId);
}
