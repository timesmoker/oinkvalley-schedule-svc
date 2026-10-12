package com.oinkvalley.schedule_svc.db.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "schedule_participants")
@IdClass(ScheduleParticipantId.class)
public class ScheduleParticipant {

	@Id
	@NotNull
	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Id
	@NotNull
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}
}
