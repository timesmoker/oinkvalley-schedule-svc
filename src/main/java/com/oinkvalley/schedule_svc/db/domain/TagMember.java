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
@Table(name = "tag_members")
@IdClass(TagMemberId.class)
public class TagMember {

	@Id
	@NotNull
	@Column(name = "tag_id", nullable = false)
	private Long tagId;

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
