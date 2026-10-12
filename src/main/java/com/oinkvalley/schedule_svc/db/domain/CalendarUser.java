package com.oinkvalley.schedule_svc.db.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** auth `users` 읽기 전용 — 이메일로 태그 검색용 */
@Entity
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class CalendarUser {

	@Id
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;
}
