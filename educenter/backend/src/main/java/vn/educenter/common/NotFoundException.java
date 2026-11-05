package vn.educenter.common;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity) {
        return new NotFoundException("Không tìm thấy " + entity);
    }
}
