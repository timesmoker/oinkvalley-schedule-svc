package com.oinkvalley.schedule_svc.config;

import com.oinkvalley.schedule_svc.security.JwtPrincipalConverter;
import com.oinkvalley.schedule_svc.security.SecurityJsonHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 스프링 시큐리티 필터 체인. board-svc 와 동일하게 JWT 무상태·JSON 401/403.
 * {@code GET /health}, {@code GET /schedules} 는 익명 허용(일정 조회는 서비스 레이어에서 PUBLIC 필터), 나머지는 인증 필요.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtPrincipalConverter jwtPrincipalConverter;
    private final SecurityJsonHandlers securityJsonHandlers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityJsonHandlers.writeUnauthorized(response))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityJsonHandlers.writeForbidden(response))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/schedules").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtPrincipalConverter))
                        .authenticationEntryPoint((request, response, authException) ->
                                securityJsonHandlers.writeInvalidToken(response))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityJsonHandlers.writeForbidden(response))
                )
                .build();
    }
}
