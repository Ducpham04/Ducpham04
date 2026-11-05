package vn.starter.zma.feedback;

import java.util.List;

/** Chuyên mục phản ánh mặc định – đổi theo nghiệp vụ của khách hàng (xem docs/CUSTOMIZE.md). */
public final class FeedbackCategories {
    public static final List<String> ALL = List.of(
            "Hạ tầng – Giao thông", "Môi trường – Vệ sinh", "An ninh – Trật tự",
            "Điện – Nước", "Dịch vụ – Chăm sóc khách hàng", "Góp ý khác");

    private FeedbackCategories() {}
}
