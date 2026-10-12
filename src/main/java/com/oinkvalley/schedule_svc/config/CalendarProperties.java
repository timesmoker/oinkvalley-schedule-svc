package com.oinkvalley.schedule_svc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 캘린더 기본 노출 태그 ID 목록.
 * {@code CALENDAR_DEFAULT_VISIBLE_TAG_IDS} — 쉼표 구분 숫자 (예: {@code 1,2,3}).
 */
@ConfigurationProperties(prefix = "calendar")
public class CalendarProperties {

	private String defaultVisibleTagIds = "";

	public List<Long> defaultVisibleTagIds() {
		if (defaultVisibleTagIds == null || defaultVisibleTagIds.isBlank()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String part : defaultVisibleTagIds.split(",")) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(trimmed));
			} catch (NumberFormatException ignored) {
				// skip invalid tokens
			}
		}
		return List.copyOf(out);
	}

	public void setDefaultVisibleTagIds(String defaultVisibleTagIds) {
		this.defaultVisibleTagIds = defaultVisibleTagIds != null ? defaultVisibleTagIds : "";
	}
}
