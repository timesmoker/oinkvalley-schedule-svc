package com.oinkvalley.schedule_svc.db.domain;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduleParticipantId implements Serializable {

	private Long scheduleId;
	private Long userId;
}
