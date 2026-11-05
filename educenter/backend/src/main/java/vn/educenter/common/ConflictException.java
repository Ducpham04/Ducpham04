package vn.educenter.common;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    private final List<?> details;

    public ConflictException(String message, List<?> details) {
        super(HttpStatus.CONFLICT, message);
        this.details = details;
    }

    public List<?> getDetails() {
        return details;
    }
}
