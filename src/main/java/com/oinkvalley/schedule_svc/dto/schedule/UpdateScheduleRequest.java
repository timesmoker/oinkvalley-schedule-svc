package com.oinkvalley.schedule_svc.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateScheduleRequest(
		@NotBlank @Size(max = 200) String title,
		List<String> tags,
		List<String> participantEmails,
		@NotBlank String startDate,
		@NotBlank String endDate,
		boolean allDay,
		String startTime,
		String endTime,
		String note,
		String link,
		String visibility
) {
}
