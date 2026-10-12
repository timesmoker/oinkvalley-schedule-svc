package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	@Query("""
			SELECT e FROM Schedule e
			WHERE e.startTime < :rangeEnd AND e.endTime > :rangeStart
			""")
	List<Schedule> findOverlapping(
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd
	);

	boolean existsByOwnerId(Long ownerId);
}
