package vn.starter.zma.storage;

import org.springframework.web.multipart.MultipartFile;

/** Lưu trữ tệp. MVP: {@link LocalStorageService}. Có thể thêm S3/MinIO bằng cách implement interface này. */
public interface StorageService {
    /** Lưu ảnh đã kiểm tra hợp lệ, trả về URL tương đối (vd: /uploads/feedback/2026/11/uuid.jpg). */
    String storeImage(MultipartFile file, String folder);
}
