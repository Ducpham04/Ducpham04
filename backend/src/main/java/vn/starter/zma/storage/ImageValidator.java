package vn.starter.zma.storage;

import org.springframework.web.multipart.MultipartFile;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;

/** Kiểm tra ảnh theo magic bytes (không tin Content-Type client gửi lên). */
public final class ImageValidator {
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ImageValidator() {}

    /** @return phần mở rộng an toàn: jpg | png | webp */
    public static String detectExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.UNSUPPORTED_FILE, "Tệp ảnh rỗng");
        if (file.getSize() > MAX_BYTES) throw new AppException(ErrorCode.PAYLOAD_TOO_LARGE, "Ảnh vượt quá 5MB");
        byte[] h = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int n = in.readNBytes(h, 0, h.length);
            if (n < 12) throw new AppException(ErrorCode.UNSUPPORTED_FILE);
        } catch (IOException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE);
        }
        return detect(h);
    }

    static String detect(byte[] h) {
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        throw new AppException(ErrorCode.UNSUPPORTED_FILE, "Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
    }
}
