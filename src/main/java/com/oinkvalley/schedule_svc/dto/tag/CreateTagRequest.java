package com.oinkvalley.schedule_svc.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
		@NotBlank @Size(max = 100) String name,
		String visibility
) {
}
