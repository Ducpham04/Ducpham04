package vn.starter.zma.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.config.AppProperties;
import vn.starter.zma.common.util.SlugUtils;
import vn.starter.zma.content.Article;
import vn.starter.zma.content.ArticleRepository;
import vn.starter.zma.content.Product;
import vn.starter.zma.content.ProductRepository;
import vn.starter.zma.feedback.Feedback;
import vn.starter.zma.feedback.FeedbackRepository;
import vn.starter.zma.feedback.FeedbackStatus;
import vn.starter.zma.user.Role;
import vn.starter.zma.user.User;
import vn.starter.zma.user.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Seed dữ liệu demo tiếng Việt khi DB trống. Tắt bằng SEED_DEMO_DATA=false.
 * Tài khoản demo: admin@demo.vn / Admin@123 · editor@demo.vn / Editor@123 · Mini App mock: zaloId "demo-user".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final AppProperties props;
    private final UserRepository users;
    private final ArticleRepository articles;
    private final ProductRepository products;
    private final FeedbackRepository feedbacks;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().demoData() || users.count() > 0) return;
        log.info("Seeding demo data (tiếng Việt)…");

        staff("admin@demo.vn", "Admin@123", "Trần Thị Hạnh", Role.ADMIN);
        staff("editor@demo.vn", "Editor@123", "Lê Minh Tuấn", Role.EDITOR);
        User citizen = new User();
        citizen.setZaloId("demo-user");
        citizen.setName("Nguyễn Văn An");
        citizen.setPhone("0912345678");
        citizen.setRole(Role.USER);
        users.save(citizen);

        article("Phường triển khai tiếp nhận phản ánh qua Zalo Mini App", "Thông báo",
                "Từ tháng 11/2026, người dân có thể gửi phản ánh, kiến nghị trực tiếp trên Zalo, kèm hình ảnh và vị trí.",
                "Nhằm nâng cao chất lượng phục vụ, UBND phường chính thức đưa vào vận hành kênh tiếp nhận phản ánh qua Zalo Mini App.\n\nNgười dân chỉ cần mở Zalo, tìm Mini App, chọn \"Gửi phản ánh\", mô tả sự việc, đính kèm tối đa 5 ảnh và vị trí. Mỗi phản ánh có mã tra cứu riêng; kết quả xử lý được thông báo qua Zalo.\n\nThời gian phản hồi cam kết: trong vòng 3 ngày làm việc.", 1);
        article("Lịch cắt điện khu vực trung tâm ngày 10/11", "Điện – Nước",
                "Điện lực thông báo tạm ngừng cấp điện để bảo trì lưới điện từ 7h30 đến 16h00.",
                "Để phục vụ công tác bảo dưỡng, sửa chữa lưới điện, Điện lực thông báo tạm ngừng cung cấp điện tại các tuyến phố: Lê Lợi, Hai Bà Trưng, Nguyễn Huệ.\n\nThời gian: 7h30 – 16h00 ngày 10/11/2026. Rất mong quý khách hàng thông cảm.", 2);
        article("Ra quân tổng vệ sinh môi trường cuối tuần", "Môi trường",
                "Hưởng ứng phong trào \"Ngày Chủ nhật xanh\", các tổ dân phố đồng loạt ra quân dọn dẹp.",
                "Sáng Chủ nhật, hơn 500 đoàn viên, thanh niên và người dân đã tham gia thu gom rác thải, phát quang bụi rậm, nạo vét cống rãnh tại 12 tổ dân phố.\n\nBan tổ chức kêu gọi người dân tiếp tục phân loại rác tại nguồn và phản ánh các điểm tập kết rác tự phát qua Mini App.", 3);
        article("Hướng dẫn đăng ký tiêm chủng mở rộng cho trẻ em", "Y tế",
                "Phụ huynh có thể đăng ký lịch tiêm cho trẻ tại trạm y tế phường từ thứ Hai đến thứ Sáu.",
                "Trạm y tế phường tổ chức tiêm chủng mở rộng định kỳ vào ngày 15 hằng tháng. Phụ huynh mang theo sổ tiêm chủng và căn cước công dân.\n\nLiên hệ: 024 3812 3456 trong giờ hành chính.", 4);
        article("Khai giảng lớp kỹ năng số cho người cao tuổi", "Giáo dục",
                "Lớp học miễn phí giúp người cao tuổi sử dụng điện thoại thông minh, Zalo và dịch vụ công trực tuyến.",
                "Khoá học gồm 6 buổi, mỗi buổi 90 phút, tại nhà văn hoá khu phố. Học viên được hướng dẫn cài đặt VNeID, thanh toán không tiền mặt và nhận diện lừa đảo trực tuyến.", 5);
        article("Điều chỉnh phân luồng giao thông đường Trần Phú", "Giao thông",
                "Từ ngày 15/11, đường Trần Phú cấm xe tải trên 2,5 tấn trong giờ cao điểm.",
                "Nhằm giảm ùn tắc, Sở Giao thông vận tải điều chỉnh phân luồng tại đường Trần Phú: cấm xe tải trên 2,5 tấn từ 6h–9h và 16h–19h hằng ngày.", 6);

        product("Gạo ST25 túi 5kg", "Gạo ST25 Sóc Trăng – gạo ngon nhất thế giới 2019, hạt dài, dẻo thơm.", 185_000, "/seed/product-1.svg", 120, true);
        product("Cà phê Robusta Đắk Lắk 500g", "Cà phê rang mộc nguyên chất, đậm vị, hậu ngọt.", 145_000, "/seed/product-2.svg", 80, true);
        product("Trà Shan Tuyết Hà Giang 200g", "Trà cổ thụ trên 100 năm tuổi, hương thơm tự nhiên.", 230_000, "/seed/product-3.svg", 50, true);
        product("Nước mắm Phú Quốc 40 độ đạm 500ml", "Nước mắm cá cơm truyền thống, ủ chượp 12 tháng.", 95_000, "/seed/product-4.svg", 200, true);
        product("Mật ong rừng U Minh 500ml", "Mật ong nguyên chất từ rừng tràm U Minh Hạ.", 260_000, "/seed/product-5.svg", 40, false);
        product("Hạt điều rang muối Bình Phước 500g", "Hạt điều loại A, rang củi giòn thơm.", 210_000, "/seed/product-6.svg", 60, false);

        feedback(citizen, "Hạ tầng – Giao thông", "Ổ gà lớn trước số 25 Lê Lợi",
                "Ổ gà sâu khoảng 20cm, rất nguy hiểm cho xe máy vào buổi tối. Đề nghị sửa chữa sớm.",
                "25 Lê Lợi", FeedbackStatus.IN_PROGRESS, "Đã chuyển đội duy tu, dự kiến khắc phục trong tuần.");
        feedback(citizen, "Môi trường – Vệ sinh", "Rác thải tập kết sai nơi quy định",
                "Đầu ngõ 12 thường xuyên có rác tập kết bừa bãi, bốc mùi hôi thối.",
                "Ngõ 12 Hai Bà Trưng", FeedbackStatus.RESOLVED, "Đã xử lý và lắp biển cấm đổ rác. Cảm ơn bạn đã phản ánh!");
        feedback(citizen, "Điện – Nước", "Đèn đường không sáng",
                "Cụm 3 đèn đường trên phố Nguyễn Huệ không sáng đã 1 tuần.",
                null, FeedbackStatus.NEW, null);
        log.info("Demo data seeded. Admin: admin@demo.vn / Admin@123 · Editor: editor@demo.vn / Editor@123");
    }

    private void staff(String email, String pw, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pw));
        u.setName(name);
        u.setRole(role);
        users.save(u);
    }

    private void article(String title, String cat, String summary, String content, int idx) {
        Article a = new Article();
        a.setTitle(title);
        a.setSlug(SlugUtils.slugify(title));
        a.setCategory(cat);
        a.setSummary(summary);
        a.setContent(content);
        a.setThumbnailUrl("/seed/news-" + idx + ".svg");
        a.setPublished(true);
        a.setPublishedAt(Instant.now().minus(idx, ChronoUnit.DAYS));
        articles.save(a);
    }

    private void product(String name, String desc, long price, String img, int stock, boolean featured) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setImageUrl(img);
        p.setStock(stock);
        p.setActive(true);
        p.setFeatured(featured);
        products.save(p);
    }

    private void feedback(User u, String cat, String title, String content, String address, FeedbackStatus st, String reply) {
        Feedback f = new Feedback();
        f.setUser(u);
        f.setCategory(cat);
        f.setTitle(title);
        f.setContent(content);
        f.setAddress(address);
        f.setStatus(st);
        f.setAdminReply(reply);
        if (address != null) {
            f.setLat(new BigDecimal("21.0287797"));
            f.setLng(new BigDecimal("105.8524247"));
        }
        feedbacks.saveAndFlush(f);
        f.setCode("PA-%d-%06d".formatted(java.time.Year.now().getValue(), f.getId()));
    }
}
