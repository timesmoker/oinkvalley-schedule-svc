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
@Table(name = "schedule_tags")
@IdClass(ScheduleTagId.class)
public class ScheduleTag {

	@Id
	@NotNull
	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Id
	@NotNull
	@Column(name = "tag_id", nullable = false)
	private Long tagId;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}
}
