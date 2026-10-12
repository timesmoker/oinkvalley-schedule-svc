package com.oinkvalley.schedule_svc.dto.tag;

import java.util.List;

public record TagListResponse(
		List<TagResponse> tags,
		List<Long> defaultVisibleTagIds
) {
}
