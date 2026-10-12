package com.oinkvalley.schedule_svc.db.domain;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TagMemberId implements Serializable {

	private Long tagId;
	private Long userId;
}
