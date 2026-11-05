package vn.bookeasy.booking;

import org.junit.jupiter.api.Test;
import vn.bookeasy.support.IntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Nhiều khách bấm cùng một khung giờ cùng lúc → đúng một người thành công (ràng buộc UNIQUE ở DB). */
class ConcurrentBookingIntegrationTest extends IntegrationTest {

    @Test
    void onlyOneOfConcurrentRequestsWins() throws Exception {
        Shop s = createShop();
        int n = 12;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String phone = String.format("09%08d", 10_000_000 + i);
            results.add(pool.submit(() -> {
                start.await();
                return book(s, s.staffA(), TOMORROW, "15:00", phone).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(statuses).filteredOn(c -> c == 201).hasSize(1);
        assertThat(statuses).filteredOn(c -> c == 409).hasSize(n - 1);
    }
}
