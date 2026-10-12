package com.oinkvalley.schedule_svc.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityJsonHandlers {

    public static final String MESSAGE_LOGIN_REQUIRED = "로그인이 필요합니다.";
    public static final String MESSAGE_MEMBER_REQUIRED = "정식 회원만 열람할 수 있습니다.";
    public static final String MESSAGE_INVALID_TOKEN = "로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요.";

    public void writeUnauthorized(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, MESSAGE_LOGIN_REQUIRED);
    }

    public void writeForbidden(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.FORBIDDEN, MESSAGE_MEMBER_REQUIRED);
    }

    public void writeInvalidToken(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, MESSAGE_INVALID_TOKEN);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":" + quoteJsonString(message) + ",\"errors\":[]}");
    }

    private static String quoteJsonString(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
