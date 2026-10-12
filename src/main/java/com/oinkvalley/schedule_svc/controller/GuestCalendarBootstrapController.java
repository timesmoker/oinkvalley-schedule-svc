package com.oinkvalley.schedule_svc.controller;

import com.oinkvalley.schedule_svc.service.GuestCalendarBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 게스트 체험용 샘플 일정. {@code POST /schedules/guest-bootstrap} — GUEST 역할만.
 * 프런트는 게스트 로그인 직후 1회 호출, 실패는 조용히 무시.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class GuestCalendarBootstrapController {

    private static final String ROLE_GUEST = "GUEST";

    private final GuestCalendarBootstrapService guestCalendarBootstrapService;

    @PostMapping("/guest-bootstrap")
    public ResponseEntity<Void> bootstrap(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        if (!hasGuestRole()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest only");
        }
        guestCalendarBootstrapService.bootstrap(userId);
        return ResponseEntity.noContent().build();
    }

    private static boolean hasGuestRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> ROLE_GUEST.equals(a) || ("ROLE_" + ROLE_GUEST).equals(a));
    }
}
