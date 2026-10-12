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
@Table(
		name = "tags",
		uniqueConstraints = @UniqueConstraint(name = "uk_tags_owner_name", columnNames = {"owner_id", "name"})
)
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ColumnDefault("nextval('tags_id_seq')")
	@Column(name = "id", nullable = false)
	private Long id;

	@NotNull
	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@NotNull
	@Size(max = 100)
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 16)
	private TagType type;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 16)
	@Builder.Default
	private Visibility visibility = Visibility.PRIVATE;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

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

	public void updateVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public void updateName(String name) {
		this.name = name;
	}
}
