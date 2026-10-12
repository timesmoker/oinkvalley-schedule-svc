package com.oinkvalley.schedule_svc.dto.tag;

import jakarta.validation.constraints.NotBlank;

public record UpdateTagVisibilityRequest(
		@NotBlank String visibility
) {
}
