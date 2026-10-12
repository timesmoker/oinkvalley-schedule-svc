package com.oinkvalley.schedule_svc.service;

import org.springframework.stereotype.Service;

/** 헬스 응답 본문만 담당하는 얇은 서비스. */
@Service
public class HealthService {

    public String status() {
        return "ok";
    }
}
