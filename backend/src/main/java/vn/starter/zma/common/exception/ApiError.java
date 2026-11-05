package vn.starter.zma.common.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(int status, String code, String message, Map<String, String> errors, String path, Instant timestamp) {
    public static ApiError of(ErrorCode code, String message, Map<String, String> errors, String path) {
        return new ApiError(code.getStatus().value(), code.name(), message, errors, path, Instant.now());
    }
}
