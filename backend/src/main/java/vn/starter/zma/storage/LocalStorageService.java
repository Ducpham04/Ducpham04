package vn.starter.zma.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {
    private final Path root;
    private final String publicPath;

    public LocalStorageService(AppProperties props) {
        this.root = Path.of(props.storage().localDir()).toAbsolutePath().normalize();
        this.publicPath = props.storage().publicPath();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục upload: " + root, e);
        }
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        if (!folder.matches("[a-z0-9-]+")) throw new IllegalArgumentException("folder không hợp lệ");
        String ext = ImageValidator.detectExtension(file);
        LocalDate d = LocalDate.now();
        String rel = "%s/%d/%02d/%s.%s".formatted(folder, d.getYear(), d.getMonthValue(), UUID.randomUUID(), ext);
        Path target = root.resolve(rel).normalize();
        if (!target.startsWith(root)) throw new AppException(ErrorCode.BAD_REQUEST);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Store file failed", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không lưu được ảnh");
        }
        return publicPath + "/" + rel;
    }
}
