package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.UserHiddenTag;
import com.oinkvalley.schedule_svc.db.domain.UserHiddenTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserHiddenTagRepository extends JpaRepository<UserHiddenTag, UserHiddenTagId> {

	@Query("SELECT u.tagId FROM UserHiddenTag u WHERE u.userId = :userId")
	List<Long> findTagIdsByUserId(@Param("userId") Long userId);

	void deleteByUserIdAndTagId(Long userId, Long tagId);

	void deleteByUserId(Long userId);

	boolean existsByUserIdAndTagId(Long userId, Long tagId);
}
