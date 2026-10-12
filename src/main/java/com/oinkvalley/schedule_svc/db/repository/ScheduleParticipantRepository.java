package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.ScheduleParticipant;
import com.oinkvalley.schedule_svc.db.domain.ScheduleParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ScheduleParticipantRepository extends JpaRepository<ScheduleParticipant, ScheduleParticipantId> {

	boolean existsByScheduleIdAndUserId(Long scheduleId, Long userId);

	@Query("SELECT ep.userId FROM ScheduleParticipant ep WHERE ep.scheduleId = :scheduleId")
	List<Long> findUserIdsByScheduleId(@Param("scheduleId") Long scheduleId);

	@Query("SELECT ep FROM ScheduleParticipant ep WHERE ep.scheduleId IN :scheduleIds")
	List<ScheduleParticipant> findByScheduleIdIn(@Param("scheduleIds") Collection<Long> scheduleIds);

	void deleteByScheduleId(Long scheduleId);
}
