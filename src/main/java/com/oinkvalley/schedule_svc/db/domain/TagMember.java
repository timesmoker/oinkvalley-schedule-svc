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

	/** 구독은 유지하되 이 태그로 들어오는 일정을 달력에서 받지 않음 */
	@Column(name = "hidden", nullable = false)
	@Builder.Default
	private boolean hidden = false;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}
}
