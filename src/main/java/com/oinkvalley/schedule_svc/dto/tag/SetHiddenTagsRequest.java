package com.oinkvalley.schedule_svc.dto.tag;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 사용자 캘린더 숨김 태그 전체 목록(교체). */
public record SetHiddenTagsRequest(@NotNull List<Long> tagIds) {
}
