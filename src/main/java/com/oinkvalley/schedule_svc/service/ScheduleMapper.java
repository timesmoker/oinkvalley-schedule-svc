package com.oinkvalley.schedule_svc.service;

import com.oinkvalley.schedule_svc.db.domain.Schedule;
import com.oinkvalley.schedule_svc.dto.schedule.ScheduleResponse;

import java.util.List;
import java.util.Map;

public final class ScheduleMapper {

	private ScheduleMapper() {
	}

	public static ScheduleResponse toResponse(
			Schedule schedule,
			List<Long> tagIds,
			List<Long> participantIds,
			List<String> participantEmails
	) {
		boolean allDay = CalendarTimeUtil.isAllDaySpan(schedule.getStartTime(), schedule.getEndTime());
		String startDate = CalendarTimeUtil.formatDate(schedule.getStartTime());
		String endDate = allDay
				? CalendarTimeUtil.formatDate(schedule.getEndTime().minusSeconds(1))
				: CalendarTimeUtil.formatDate(schedule.getEndTime());
		if (endDate.compareTo(startDate) < 0) {
			endDate = startDate;
		}

		return new ScheduleResponse(
				schedule.getId(),
				schedule.getTitle(),
				tagIds.stream().map(String::valueOf).toList(),
				schedule.getOwnerId(),
				participantIds,
				participantEmails == null ? List.of() : participantEmails,
				startDate,
				endDate,
				allDay,
				allDay ? null : CalendarTimeUtil.formatTime(schedule.getStartTime()),
				allDay ? null : CalendarTimeUtil.formatTime(schedule.getEndTime()),
				schedule.getDescription(),
				schedule.getLink(),
				schedule.getVisibility().name()
		);
	}

	public static Map<Long, List<Long>> tagIdsBySchedule(List<com.oinkvalley.schedule_svc.db.domain.ScheduleTag> links) {
		return links.stream().collect(
				java.util.stream.Collectors.groupingBy(
						com.oinkvalley.schedule_svc.db.domain.ScheduleTag::getScheduleId,
						java.util.stream.Collectors.mapping(
								com.oinkvalley.schedule_svc.db.domain.ScheduleTag::getTagId,
								java.util.stream.Collectors.toList()
						)
				)
		);
	}

	public static Map<Long, List<Long>> participantIdsBySchedule(
			List<com.oinkvalley.schedule_svc.db.domain.ScheduleParticipant> links
	) {
		return links.stream().collect(
				java.util.stream.Collectors.groupingBy(
						com.oinkvalley.schedule_svc.db.domain.ScheduleParticipant::getScheduleId,
						java.util.stream.Collectors.mapping(
								com.oinkvalley.schedule_svc.db.domain.ScheduleParticipant::getUserId,
								java.util.stream.Collectors.toList()
						)
				)
		);
	}
}
