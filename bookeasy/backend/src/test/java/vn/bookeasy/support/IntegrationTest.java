package vn.bookeasy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.bookeasy.config.TimeConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nền cho integration test: Spring context thật + H2 (MODE=MySQL) + Flyway + MockMvc.
 * Thời gian cố định Thứ hai 07/01/2030 08:00 (giờ VN).
 */
@SpringBootTest
@org.springframework.context.annotation.Import(IntegrationTest.ClockConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {
    public static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 7, 8, 0);
    public static final LocalDate TOMORROW = NOW.toLocalDate().plusDays(1);

    @TestConfiguration
    public static class ClockConfig {
        @Bean
        @Primary
        public MutableClock testClock() {
            return new MutableClock(NOW, TimeConfig.ZONE);
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected ResultActions call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        return mvc.perform(req);
    }

    protected JsonNode ok(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        String res = call(req, token, body).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return res.isEmpty() ? null : json.readTree(res);
    }

    protected JsonNode fetch(String url, String token) throws Exception {
        return ok(get(url), token, null);
    }

    // ------------------------------------------------------------------ Fixtures

    /** Một cơ sở hoàn chỉnh: chủ, 2 nhân viên (ca 08:00–18:00 mọi ngày), 1 dịch vụ 60 phút. */
    public record Shop(String slug, String ownerToken, long branchId, long serviceId, long staffA, long staffB,
                       long categoryId) {
    }

    protected Shop createShop() throws Exception {
        String slug = "shop-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode reg = ok(post("/api/auth/register"), null, Map.of(
                "businessName", "Spa Thử Nghiệm", "slug", slug, "category", "SPA",
                "fullName", "Chủ Thử", "email", slug + "@test.vn", "password", "Matkhau123"));
        String token = reg.get("accessToken").asText();
        long branchId = fetch("/api/branches", token).get(0).get("id").asLong();
        long catId = ok(post("/api/service-categories"), token, Map.of("name", "Massage", "sortOrder", 1)).get("id").asLong();
        long a = createStaff(token, branchId, "Nguyễn Thị A");
        long b = createStaff(token, branchId, "Trần Thị B");
        long svc = ok(post("/api/services"), token, Map.of("name", "Massage 60 phút", "categoryId", catId,
                "durationMinutes", 60, "price", 300000, "active", true, "sortOrder", 0, "staffIds", List.of(a, b)))
                .get("id").asLong();
        return new Shop(slug, token, branchId, svc, a, b, catId);
    }

    protected long createStaff(String token, long branchId, String name) throws Exception {
        long id = ok(post("/api/staff"), token, Map.of("fullName", name, "branchId", branchId, "color", "#2f7d6b"))
                .get("id").asLong();
        List<Map<String, Object>> shifts = new java.util.ArrayList<>();
        for (int d = 1; d <= 7; d++) shifts.add(Map.of("dayOfWeek", d, "startTime", "08:00", "endTime", "18:00"));
        ok(put("/api/staff/" + id + "/shifts"), token, Map.of("shifts", shifts));
        return id;
    }

    protected Map<String, Object> publicBooking(Shop s, Long staffId, LocalDate date, String time, String phone) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("branchId", s.branchId());
        m.put("serviceId", s.serviceId());
        m.put("staffId", staffId);
        m.put("date", date.toString());
        m.put("time", time);
        m.put("fullName", "Khách Hàng");
        m.put("phone", phone);
        m.put("email", "khach@example.com");
        return m;
    }

    protected ResultActions book(Shop s, Long staffId, LocalDate date, String time, String phone) throws Exception {
        return call(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, staffId, date, time, phone));
    }

    protected String login(String email, String password) throws Exception {
        return ok(post("/api/auth/login"), null, Map.of("email", email, "password", password)).get("accessToken").asText();
    }
}
