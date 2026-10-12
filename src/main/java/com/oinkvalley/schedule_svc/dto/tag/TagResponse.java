package com.oinkvalley.schedule_svc.dto.tag;

public record TagResponse(
		long id,
		String name,
		String type,
		String visibility,
		long ownerId,
		boolean hidden
) {
}
