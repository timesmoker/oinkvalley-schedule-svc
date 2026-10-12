package com.oinkvalley.schedule_svc.dto.tag;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddTagMemberRequest(
		@NotBlank @Email String email
) {
}
