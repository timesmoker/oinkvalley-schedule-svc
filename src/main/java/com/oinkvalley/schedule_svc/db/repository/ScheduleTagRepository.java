package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.ScheduleTag;
import com.oinkvalley.schedule_svc.db.domain.ScheduleTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ScheduleTagRepository extends JpaRepository<ScheduleTag, ScheduleTagId> {

	List<ScheduleTag> findByScheduleId(Long scheduleId);

	@Query("SELECT et.tagId FROM ScheduleTag et WHERE et.scheduleId = :scheduleId")
	List<Long> findTagIdsByScheduleId(@Param("scheduleId") Long scheduleId);

	@Query("SELECT et FROM ScheduleTag et WHERE et.scheduleId IN :scheduleIds")
	List<ScheduleTag> findByScheduleIdIn(@Param("scheduleIds") Collection<Long> scheduleIds);

	List<ScheduleTag> findByTagId(Long tagId);

	void deleteByScheduleIdAndTagId(Long scheduleId, Long tagId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM ScheduleTag et WHERE et.scheduleId = :scheduleId")
	void deleteAllByScheduleId(@Param("scheduleId") Long scheduleId);
}
