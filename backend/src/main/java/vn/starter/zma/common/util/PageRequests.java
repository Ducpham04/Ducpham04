package vn.starter.zma.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {
    public static final int MAX_SIZE = 50;

    private PageRequests() {}

    public static Pageable of(int page, int size, Sort sort) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(p, s, sort);
    }

    /** Escape ký tự đặc biệt của LIKE và bọc %…%. Trả null nếu chuỗi rỗng. */
    public static String likePattern(String q) {
        if (q == null || q.isBlank()) return null;
        String escaped = q.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
