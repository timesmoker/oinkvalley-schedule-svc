package com.oinkvalley.schedule_svc.controller;

import com.oinkvalley.schedule_svc.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로드밸런서·오케스트레이션용 최소 헬스 엔드포인트. */
@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(healthService.status());
    }
}
