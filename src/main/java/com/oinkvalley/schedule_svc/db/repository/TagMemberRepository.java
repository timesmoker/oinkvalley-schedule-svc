package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.TagMember;
import com.oinkvalley.schedule_svc.db.domain.TagMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagMemberRepository extends JpaRepository<TagMember, TagMemberId> {

	boolean existsByTagIdAndUserId(Long tagId, Long userId);

	@Query("SELECT tm.tagId FROM TagMember tm WHERE tm.userId = :userId")
	List<Long> findTagIdsByUserId(@Param("userId") Long userId);

	void deleteByTagIdAndUserId(Long tagId, Long userId);
}
