package vn.educenter.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
    }
}
