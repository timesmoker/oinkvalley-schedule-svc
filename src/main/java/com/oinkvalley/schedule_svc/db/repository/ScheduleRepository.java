package com.oinkvalley.schedule_svc.db.repository;

import com.oinkvalley.schedule_svc.db.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	/**
	 * 로그인 사용자의 달력 일정 id. 열쇠(내 일정·구독 태그·참여)에서 출발해 기간 인덱스로 이번 기간만 읽는다.
	 * <ul>
	 *   <li>내 일정: 붙은 태그를 내가 전부 숨기지 않았으면 (태그가 없어도 보임)</li>
	 *   <li>구독 태그: tag_members 에서 숨기지 않은 태그 (남의 태그는 PRIVATE 가 아닐 때만). 남의 PRIVATE 일정은 제외</li>
	 *   <li>참여 일정: 숨김과 무관</li>
	 * </ul>
	 * 연결 테이블의 start_time/end_time 은 schedules 사본 (DB 트리거가 맞춤).
	 */
	@Query(nativeQuery = true, value = """
			WITH keyring AS (
			    SELECT m.tag_id FROM tag_members m JOIN tags t ON t.id = m.tag_id
			    WHERE m.user_id = :userId AND NOT m.hidden
			      AND (t.owner_id = :userId OR t.visibility <> 'PRIVATE')
			)
			SELECT s.id FROM schedules s
			WHERE s.owner_id = :userId
			  AND s.end_time > :rangeStart AND s.start_time < :rangeEnd
			  AND NOT (
			      EXISTS (
			          SELECT 1 FROM schedule_tags st
			          WHERE st.schedule_id = s.id)
			      AND NOT EXISTS (
			          SELECT 1 FROM schedule_tags st
			          WHERE st.schedule_id = s.id
			            AND NOT EXISTS (
			                SELECT 1 FROM tag_members h
			                WHERE h.user_id = :userId AND h.tag_id = st.tag_id AND h.hidden)))
			UNION
			SELECT st.schedule_id FROM keyring k
			JOIN schedule_tags st ON st.tag_id = k.tag_id
			JOIN schedules s ON s.id = st.schedule_id
			WHERE st.end_time > :rangeStart AND st.start_time < :rangeEnd
			  AND s.visibility <> 'PRIVATE'
			UNION
			SELECT sp.schedule_id FROM schedule_participants sp
			WHERE sp.user_id = :userId
			  AND sp.end_time > :rangeStart AND sp.start_time < :rangeEnd
			""")
	List<Long> findVisibleIds(
			@Param("userId") long userId,
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd
	);

	/** 비로그인 달력: 이번 기간의 PUBLIC 일정 id */
	@Query(nativeQuery = true, value = """
			SELECT s.id FROM schedules s
			WHERE s.visibility = 'PUBLIC'
			  AND s.end_time > :rangeStart AND s.start_time < :rangeEnd
			""")
	List<Long> findPublicIds(
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd
	);

	boolean existsByOwnerId(Long ownerId);
}
