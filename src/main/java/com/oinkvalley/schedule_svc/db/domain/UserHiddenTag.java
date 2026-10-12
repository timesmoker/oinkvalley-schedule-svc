package com.oinkvalley.schedule_svc.db.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/** 사용자별 캘린더에서 숨긴 태그. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "user_hidden_tags")
@IdClass(UserHiddenTagId.class)
public class UserHiddenTag {

	@Id
	@NotNull
	@Column(name = "user_id", nullable = false)
	private Long userId;

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
