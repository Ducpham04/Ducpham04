package vn.starter.zma.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode code;

    public AppException(ErrorCode code) {
        this(code, code.getDefaultMessage());
    }

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public static AppException notFound(String what) {
        return new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy " + what);
    }
}
