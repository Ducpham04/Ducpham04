package vn.starter.zma.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.starter.zma.content.ArticleRepository;
import vn.starter.zma.content.ProductRepository;
import vn.starter.zma.feedback.FeedbackRepository;
import vn.starter.zma.feedback.FeedbackStatus;
import vn.starter.zma.order.OrderRepository;
import vn.starter.zma.order.OrderStatus;
import vn.starter.zma.user.Role;
import vn.starter.zma.user.UserRepository;

import java.util.EnumMap;
import java.util.Map;

@Tag(name = "Admin - Stats")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class StatsController {
    private final UserRepository users;
    private final FeedbackRepository feedbacks;
    private final OrderRepository orders;
    private final ArticleRepository articles;
    private final ProductRepository products;

    @GetMapping
    @Transactional(readOnly = true)
    public StatsDto stats() {
        Map<FeedbackStatus, Long> byStatus = new EnumMap<>(FeedbackStatus.class);
        for (FeedbackStatus s : FeedbackStatus.values()) byStatus.put(s, 0L);
        for (Object[] row : feedbacks.countGroupByStatus()) byStatus.put((FeedbackStatus) row[0], (Long) row[1]);
        return new StatsDto(users.countByRole(Role.USER), articles.count(), products.count(), byStatus,
                orders.countByStatus(OrderStatus.PAID), orders.countByStatus(OrderStatus.PENDING), orders.sumPaidAmount());
    }

    public record StatsDto(long users, long articles, long products, Map<FeedbackStatus, Long> feedbackByStatus,
                           long paidOrders, long pendingOrders, long revenue) {}
}
