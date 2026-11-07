package vn.bookeasy.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import vn.bookeasy.support.IntegrationTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Người dùng cơ sở B không được đọc/ghi dữ liệu của cơ sở A. */
class TenantIsolationIntegrationTest extends IntegrationTest {

    @Test
    void ownersCannotTouchOtherTenants() throws Exception {
        Shop a = createShop();
        Shop b = createShop();
        JsonNode booking = ok(post("/api/bookings"), a.ownerToken(), Map.of("serviceId", a.serviceId(), "staffId", a.staffA(),
                "startAt", TOMORROW + "T09:00:00", "fullName", "Khách A", "phone", "0901234567"));
        long bookingId = booking.get("id").asLong();
        long customerId = booking.get("customer").get("id").asLong();

        call(get("/api/bookings/" + bookingId), b.ownerToken(), null).andExpect(status().isNotFound());
        call(post("/api/bookings/" + bookingId + "/status"), b.ownerToken(), Map.of("status", "CANCELLED")).andExpect(status().isNotFound());
        call(get("/api/customers/" + customerId), b.ownerToken(), null).andExpect(status().isNotFound());
        call(put("/api/services/" + a.serviceId()), b.ownerToken(), Map.of("name", "Hack", "durationMinutes", 30, "price", 0,
                "active", true, "sortOrder", 0, "staffIds", List.of())).andExpect(status().isNotFound());
        call(get("/api/staff/" + a.staffA()), b.ownerToken(), null).andExpect(status().isNotFound());
        call(put("/api/branches/" + a.branchId()), b.ownerToken(), Map.of("name", "Hack")).andExpect(status().isNotFound());
        // B không thể gán nhân viên của A vào dịch vụ của mình
        call(post("/api/services"), b.ownerToken(), Map.of("name", "X", "durationMinutes", 30, "price", 0, "active", true,
                "sortOrder", 0, "staffIds", List.of(a.staffA()))).andExpect(status().isNotFound());
        // B không đặt lịch được với nhân viên của A qua API nội bộ
        call(post("/api/bookings"), b.ownerToken(), Map.of("serviceId", b.serviceId(), "staffId", a.staffA(),
                "startAt", TOMORROW + "T10:00:00", "fullName", "X", "phone", "0901234567")).andExpect(status().isNotFound());

        assertThat(fetch("/api/bookings?from=" + TOMORROW + "T00:00:00&to=" + TOMORROW.plusDays(1) + "T00:00:00", b.ownerToken())).isEmpty();
    }

    @Test
    void suspendedBusinessCannotTakeBookingsOrLogin() throws Exception {
        Shop a = createShop();
        // tạo super admin trực tiếp qua repository là không cần: dùng API admin với tài khoản seed không có trong test,
        // nên kiểm tra qua service layer bằng cách gọi endpoint với quyền SUPER_ADMIN giả lập.
        String admin = superAdminToken();
        JsonNode list = fetch("/api/admin/businesses", admin);
        long id = -1;
        for (JsonNode n : list) if (n.get("slug").asText().equals(a.slug())) id = n.get("id").asLong();
        ok(patch("/api/admin/businesses/" + id + "/status"), admin, Map.of("status", "SUSPENDED"));
        call(get("/api/public/businesses/" + a.slug()), null, null).andExpect(status().isForbidden());
        call(post("/api/auth/login"), null, Map.of("email", a.slug() + "@test.vn", "password", "Matkhau123")).andExpect(status().isForbidden());
    }

    @org.springframework.beans.factory.annotation.Autowired
    vn.bookeasy.auth.UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.security.crypto.password.PasswordEncoder encoder;

    private String superAdminToken() throws Exception {
        if (users.findByEmailIgnoreCase("root@bookeasy.test").isEmpty()) {
            vn.bookeasy.auth.User u = new vn.bookeasy.auth.User();
            u.setEmail("root@bookeasy.test");
            u.setFullName("Root");
            u.setRole(vn.bookeasy.auth.Role.SUPER_ADMIN);
            u.setPasswordHash(encoder.encode("Matkhau123"));
            users.save(u);
        }
        return login("root@bookeasy.test", "Matkhau123");
    }
}
