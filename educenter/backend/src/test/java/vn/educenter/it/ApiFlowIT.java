package vn.educenter.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.mysql.MySQLContainer;
import vn.educenter.security.Role;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;

/** Luồng chính end-to-end ở tầng API trên MySQL thật (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFlowIT {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> MYSQL.getJdbcUrl() + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh");
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    String superToken, ownerA, ownerB, teacherA, portalA, refreshA;
    long teacherId, roomId, courseId, classId, studentId, sessionId, invoiceId, paymentId;
    final LocalDate monday = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).with(DayOfWeek.MONDAY).minusWeeks(1);

    @BeforeAll
    void createSuperAdmin() {
        User u = new User();
        u.setEmail("root@educenter.vn");
        u.setPasswordHash(encoder.encode("Root@12345"));
        u.setFullName("Root");
        u.setRole(Role.SUPER_ADMIN);
        u.setEnabled(true);
        users.save(u);
    }

    // ---------------------------------------------------------------- helpers

    JsonNode json(ResultActions r) throws Exception {
        String body = r.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return body.isEmpty() ? null : om.readTree(body);
    }

    ResultActions call(String method, String url, String token, Object body) throws Exception {
        var req = request(org.springframework.http.HttpMethod.valueOf(method), url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.content(om.writeValueAsString(body));
        return mvc.perform(req);
    }

    JsonNode ok(String method, String url, String token, Object body) throws Exception {
        return json(call(method, url, token, body).andExpect(status().is2xxSuccessful()));
    }

    String login(String email, String pw) throws Exception {
        JsonNode n = ok("POST", "/api/v1/auth/login", null, Map.of("email", email, "password", pw));
        if (email.startsWith("owner@a")) refreshA = n.get("refreshToken").asText();
        return n.get("accessToken").asText();
    }

    // ---------------------------------------------------------------- tests

    @Test @Order(1)
    void superAdminCreatesTwoTenants() throws Exception {
        call("POST", "/api/v1/auth/login", null, Map.of("email", "root@educenter.vn", "password", "sai-mat-khau"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không đúng"));
        superToken = login("root@educenter.vn", "Root@12345");
        for (String t : new String[]{"a", "b"}) {
            ok("POST", "/api/v1/admin/tenants", superToken, Map.of("name", "Trung tâm " + t.toUpperCase(), "slug", "tt-" + t, "plan", "BASIC",
                    "owner", Map.of("fullName", "Chủ " + t, "email", "owner@" + t + ".vn", "password", "Owner@123")));
        }
        call("POST", "/api/v1/admin/tenants", superToken, Map.of("name", "Trùng", "slug", "tt-a", "plan", "BASIC",
                "owner", Map.of("fullName", "X", "email", "x@x.vn", "password", "Owner@123"))).andExpect(status().isBadRequest());
        ownerA = login("owner@a.vn", "Owner@123");
        ownerB = login("owner@b.vn", "Owner@123");
        // Chủ trung tâm không gọi được API quản trị hệ thống
        call("GET", "/api/v1/admin/tenants", ownerA, null).andExpect(status().isForbidden());
    }

    @Test @Order(2)
    void ownerSetsUpCatalogStaffAndBank() throws Exception {
        teacherId = ok("POST", "/api/v1/users", ownerA, Map.of("fullName", "Cô Hạnh", "email", "hanh@a.vn", "role", "TEACHER", "password", "Teacher@123")).get("id").asLong();
        roomId = ok("POST", "/api/v1/rooms", ownerA, Map.of("name", "P101", "capacity", 20)).get("id").asLong();
        courseId = ok("POST", "/api/v1/courses", ownerA, Map.of("code", "IELTS-F", "name", "IELTS Foundation", "totalSessions", 36, "tuitionFee", 7200000)).get("id").asLong();
        ok("PUT", "/api/v1/center", ownerA, Map.of("name", "Trung tâm A", "bankCode", "VCB", "bankAccountNo", "1023456789", "bankAccountName", "TRUNG TAM A"));
        call("POST", "/api/v1/rooms", ownerA, Map.of("name", "", "capacity", 0)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test @Order(3)
    void classScheduleGeneratesSessionsAndDetectsConflicts() throws Exception {
        JsonNode c = ok("POST", "/api/v1/classes", ownerA, Map.of("courseId", courseId, "code", "IELTS-K1", "name", "IELTS K1", "teacherId", teacherId,
                "roomId", roomId, "maxStudents", 2, "startDate", monday.toString(), "endDate", monday.plusWeeks(4).toString(), "status", "ONGOING"));
        classId = c.get("id").asLong();
        JsonNode detail = ok("PUT", "/api/v1/classes/" + classId + "/schedules", ownerA, Map.of("slots", java.util.List.of(
                Map.of("dayOfWeek", 1, "startTime", "18:00", "endTime", "19:30"),
                Map.of("dayOfWeek", 3, "startTime", "18:00", "endTime", "19:30"))));
        assertThat(detail.get("totalSessions").asLong()).isGreaterThan(0);

        long other = ok("POST", "/api/v1/classes", ownerA, Map.of("courseId", courseId, "code", "IELTS-K2", "name", "IELTS K2", "roomId", roomId,
                "maxStudents", 10, "startDate", monday.toString(), "status", "PLANNED")).get("id").asLong();
        call("PUT", "/api/v1/classes/" + other + "/schedules", ownerA, Map.of("slots", java.util.List.of(
                Map.of("dayOfWeek", 1, "startTime", "19:00", "endTime", "20:30"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts[0].type").value("ROOM"))
                .andExpect(jsonPath("$.conflicts[0].classCode").value("IELTS-K1"));
        ok("PUT", "/api/v1/classes/" + other + "/schedules", ownerA, Map.of("slots", java.util.List.of(
                Map.of("dayOfWeek", 1, "startTime", "19:30", "endTime", "21:00"))));
    }

    @Test @Order(4)
    void enrollRespectsCapacityAndDuplicates() throws Exception {
        studentId = ok("POST", "/api/v1/students", ownerA, Map.of("fullName", "Nguyễn Gia Hân", "phone", "0908111222", "joinedAt", monday.toString())).get("id").asLong();
        long s2 = ok("POST", "/api/v1/students", ownerA, Map.of("fullName", "Trần Minh Khang")).get("id").asLong();
        long s3 = ok("POST", "/api/v1/students", ownerA, Map.of("fullName", "Lê Bảo Anh")).get("id").asLong();
        ok("POST", "/api/v1/students/" + studentId + "/guardians", ownerA, Map.of("fullName", "Nguyễn Văn Hùng", "phone", "0908111333", "email", "bo@a.vn", "primary", true));
        ok("POST", "/api/v1/classes/" + classId + "/enrollments", ownerA, Map.of("studentId", studentId, "startDate", monday.toString()));
        call("POST", "/api/v1/classes/" + classId + "/enrollments", ownerA, Map.of("studentId", studentId)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Học viên đã có trong lớp"));
        ok("POST", "/api/v1/classes/" + classId + "/enrollments", ownerA, Map.of("studentId", s2, "startDate", monday.toString()));
        call("POST", "/api/v1/classes/" + classId + "/enrollments", ownerA, Map.of("studentId", s3)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("đủ sĩ số")));
        call("POST", "/api/v1/students", ownerA, Map.of("fullName", "X", "phone", "12345")).andExpect(status().isBadRequest());
    }

    @Test @Order(5)
    void teacherTakesAttendanceOnlyForOwnClass() throws Exception {
        teacherA = login("hanh@a.vn", "Teacher@123");
        JsonNode sessions = ok("GET", "/api/v1/sessions?from=" + monday + "&to=" + monday.plusDays(6), teacherA, null);
        assertThat(sessions.size()).isGreaterThanOrEqualTo(2);
        sessionId = sessions.get(0).get("id").asLong();
        JsonNode roster = ok("GET", "/api/v1/sessions/" + sessionId + "/attendance", teacherA, null);
        assertThat(roster.get("items").size()).isEqualTo(2);
        JsonNode saved = ok("PUT", "/api/v1/sessions/" + sessionId + "/attendance", teacherA, Map.of("items", java.util.List.of(
                Map.of("studentId", roster.get("items").get(0).get("studentId").asLong(), "status", "PRESENT"),
                Map.of("studentId", roster.get("items").get(1).get("studentId").asLong(), "status", "ABSENT", "note", "Ốm"))));
        assertThat(saved.get("taken").asBoolean()).isTrue();
        assertThat(saved.get("session").get("status").asText()).isEqualTo("COMPLETED");
        // Giáo viên không xem được học phí / quản lý học viên
        call("GET", "/api/v1/debts", teacherA, null).andExpect(status().isForbidden());
        call("POST", "/api/v1/students", teacherA, Map.of("fullName", "Hack")).andExpect(status().isForbidden());
        JsonNode stats = ok("GET", "/api/v1/students/" + studentId + "/attendance", ownerA, null);
        assertThat(stats.get("total").asInt()).isEqualTo(1);
    }

    @Test @Order(6)
    void invoicePartialPaymentsReceiptAndQr() throws Exception {
        JsonNode inv = ok("POST", "/api/v1/invoices", ownerA, Map.of("studentId", studentId, "classId", classId, "amount", 7200000, "discount", 200000,
                "dueDate", monday.plusDays(2).toString()));
        invoiceId = inv.get("id").asLong();
        assertThat(inv.get("total").asLong()).isEqualTo(7000000);
        assertThat(inv.get("code").asText()).startsWith("HD");
        call("POST", "/api/v1/invoices/" + invoiceId + "/payments", ownerA, Map.of("amount", 8000000, "method", "CASH"))
                .andExpect(status().isBadRequest());
        JsonNode p1 = ok("POST", "/api/v1/invoices/" + invoiceId + "/payments", ownerA, Map.of("amount", 3000000, "method", "CASH"));
        assertThat(p1.get("status").asText()).isEqualTo("PARTIAL");
        assertThat(p1.get("overdue").asBoolean()).isTrue();
        paymentId = p1.get("payments").get(0).get("id").asLong();
        JsonNode qr = ok("GET", "/api/v1/invoices/" + invoiceId + "/vietqr", ownerA, null);
        assertThat(qr.get("imageUrl").asText()).contains("img.vietqr.io/image/VCB-1023456789").contains("amount=4000000");
        JsonNode debts = ok("GET", "/api/v1/debts", ownerA, null);
        assertThat(debts.get("totalOutstanding").asLong()).isEqualTo(4000000);
        var pdf = mvc.perform(get("/api/v1/payments/" + paymentId + "/receipt.pdf").header("Authorization", "Bearer " + ownerA))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        JsonNode p2 = ok("POST", "/api/v1/invoices/" + invoiceId + "/payments", ownerA, Map.of("amount", 4000000, "method", "BANK_TRANSFER"));
        assertThat(p2.get("status").asText()).isEqualTo("PAID");
        call("POST", "/api/v1/invoices/" + invoiceId + "/cancel", ownerA, null).andExpect(status().isBadRequest());
        JsonNode dash = ok("GET", "/api/v1/reports/dashboard", ownerA, null);
        assertThat(dash.get("revenueThisMonth").asLong()).isGreaterThanOrEqualTo(0);
        var xlsx = mvc.perform(get("/api/v1/reports/export/revenue.xlsx").header("Authorization", "Bearer " + ownerA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(xlsx[0]).isEqualTo((byte) 'P'); // zip
    }

    @Test @Order(7)
    void tenantsAreIsolated() throws Exception {
        call("GET", "/api/v1/students/" + studentId, ownerB, null).andExpect(status().isNotFound());
        call("GET", "/api/v1/classes/" + classId, ownerB, null).andExpect(status().isNotFound());
        call("GET", "/api/v1/invoices/" + invoiceId, ownerB, null).andExpect(status().isNotFound());
        call("POST", "/api/v1/invoices/" + invoiceId + "/payments", ownerB, Map.of("amount", 1000, "method", "CASH")).andExpect(status().isNotFound());
        call("GET", "/api/v1/payments/" + paymentId + "/receipt.pdf", ownerB, null).andExpect(status().isNotFound());
        assertThat(ok("GET", "/api/v1/students", ownerB, null).get("totalItems").asInt()).isZero();
        assertThat(ok("GET", "/api/v1/debts", ownerB, null).get("studentCount").asInt()).isZero();
        // Tenant B dùng được tên phòng / mã khóa trùng với A
        ok("POST", "/api/v1/rooms", ownerB, Map.of("name", "P101", "capacity", 10));
    }

    @Test @Order(8)
    void portalSeesOnlyOwnData() throws Exception {
        ok("POST", "/api/v1/students/" + studentId + "/portal-account", ownerA, Map.of("email", "ph@a.vn", "password", "Parent@123"));
        portalA = login("ph@a.vn", "Parent@123");
        JsonNode me = ok("GET", "/api/v1/portal/me", portalA, null);
        assertThat(me.get("fullName").asText()).isEqualTo("Nguyễn Gia Hân");
        assertThat(ok("GET", "/api/v1/portal/invoices", portalA, null).size()).isEqualTo(1);
        assertThat(ok("GET", "/api/v1/portal/attendance", portalA, null).get("total").asInt()).isEqualTo(1);
        call("GET", "/api/v1/students", portalA, null).andExpect(status().isForbidden());
        call("GET", "/api/v1/invoices/" + invoiceId, portalA, null).andExpect(status().isForbidden());
        call("GET", "/api/v1/portal/me", ownerA, null).andExpect(status().isForbidden());
    }

    @Test @Order(9)
    void refreshTokenRotatesAndReuseIsRejected() throws Exception {
        JsonNode r1 = ok("POST", "/api/v1/auth/refresh", null, Map.of("refreshToken", refreshA));
        String newRefresh = r1.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(refreshA);
        call("POST", "/api/v1/auth/refresh", null, Map.of("refreshToken", refreshA)).andExpect(status().isUnauthorized());
        // Dùng lại token cũ → thu hồi cả phiên mới
        call("POST", "/api/v1/auth/refresh", null, Map.of("refreshToken", newRefresh)).andExpect(status().isUnauthorized());
        call("GET", "/api/v1/students", null, null).andExpect(status().isUnauthorized());
    }

    @Test @Order(10)
    void suspendedTenantCannotLogin() throws Exception {
        long tenantB = ok("GET", "/api/v1/admin/tenants", superToken, null).findValues("slug").stream()
                .filter(n -> n.asText().equals("tt-b")).count();
        assertThat(tenantB).isEqualTo(1);
        JsonNode list = ok("GET", "/api/v1/admin/tenants", superToken, null);
        long id = 0;
        for (JsonNode t : list) if (t.get("slug").asText().equals("tt-b")) id = t.get("id").asLong();
        ok("PATCH", "/api/v1/admin/tenants/" + id + "/status", superToken, Map.of("status", "SUSPENDED"));
        call("POST", "/api/v1/auth/login", null, Map.of("email", "owner@b.vn", "password", "Owner@123")).andExpect(status().isUnauthorized());
    }
}
