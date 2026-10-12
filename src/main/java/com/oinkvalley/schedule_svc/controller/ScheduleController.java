package com.oinkvalley.schedule_svc.controller;

import com.oinkvalley.schedule_svc.dto.schedule.CreateScheduleRequest;
import com.oinkvalley.schedule_svc.dto.schedule.ScheduleResponse;
import com.oinkvalley.schedule_svc.dto.schedule.UpdateScheduleRequest;
import com.oinkvalley.schedule_svc.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * @param scope {@code visible}(기본) — 볼 수 있는 일정, {@code mine} — 소유·참여 일정 (로그인 필요)
     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String tagIds,
            @RequestParam(defaultValue = "visible") String scope
    ) {
        if ("mine".equalsIgnoreCase(scope) && userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required for scope=mine");
        }
        return ResponseEntity.ok(scheduleService.listVisible(userId, from, to, parseTagIds(tagIds), scope));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(scheduleService.create(userId, request));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable long scheduleId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(scheduleService.update(userId, scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            @PathVariable long scheduleId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        scheduleService.delete(userId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    private static List<Long> parseTagIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tagIds: " + raw);
            }
        }
        return List.copyOf(ids);
    }
}
