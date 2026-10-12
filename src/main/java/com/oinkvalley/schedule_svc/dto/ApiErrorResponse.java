package com.oinkvalley.schedule_svc.dto;

import java.util.List;

public record ApiErrorResponse(String message, List<FieldError> errors) {

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(message, List.of());
    }

    public record FieldError(String field, String message) {
    }
}
