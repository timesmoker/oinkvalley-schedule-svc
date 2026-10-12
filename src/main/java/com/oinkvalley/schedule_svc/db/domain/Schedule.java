package com.oinkvalley.schedule_svc.db.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "schedules")
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ColumnDefault("nextval('schedules_id_seq')")
	@Column(name = "id", nullable = false)
	private Long id;

	@NotNull
	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@NotNull
	@Size(max = 200)
	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Size(max = 2048)
	@Column(name = "link", length = 2048)
	private String link;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 16)
	@Builder.Default
	private Visibility visibility = Visibility.PRIVATE;

	@NotNull
	@Column(name = "start_time", nullable = false)
	private Instant startTime;

	@NotNull
	@Column(name = "end_time", nullable = false)
	private Instant endTime;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = Instant.now();
	}

	public void updateDetails(
			String title,
			String description,
			String link,
			Visibility visibility,
			Instant startTime,
			Instant endTime
	) {
		this.title = title;
		this.description = description;
		this.link = link;
		this.visibility = visibility;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
