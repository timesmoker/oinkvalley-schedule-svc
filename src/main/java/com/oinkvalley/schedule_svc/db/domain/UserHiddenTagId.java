package com.oinkvalley.schedule_svc.db.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserHiddenTagId implements Serializable {

	private Long userId;
	private Long tagId;
}
