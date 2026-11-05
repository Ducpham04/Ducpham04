package vn.bookeasy.booking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.bookeasy.notification.BookingScheduler;
import vn.bookeasy.notification.NotificationLogRepository;
import vn.bookeasy.support.IntegrationTest;

import java.time.Duration;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowIntegrationTest extends IntegrationTest {
    @Autowired
    BookingScheduler scheduler;
    @Autowired
    BookingRepository bookings;
    @Autowired
    NotificationLogRepository notificationLogs;

    private JsonNode slots(Shop s, Long staffId) throws Exception {
        return fetch("/api/public/businesses/" + s.slug() + "/availability?serviceId=" + s.serviceId() + "&branchId=" + s.branchId()
                + "&date=" + TOMORROW + (staffId == null ? "" : "&staffId=" + staffId), null);
    }

    private boolean hasTime(JsonNode slots, String time) {
        return StreamSupport.stream(slots.spliterator(), false).anyMatch(n -> n.get("time").asText().startsWith(time));
    }

    @Test
    void publicPageShowsBusinessServicesAndAvailability() throws Exception {
        Shop s = createShop();
        JsonNode biz = fetch("/api/public/businesses/" + s.slug(), null);
        assertThat(biz.get("services")).hasSize(1);
        assertThat(biz.get("branches")).hasSize(1);
        JsonNode slots = slots(s, null);
        // ca 08:00–18:00, dịch vụ 60' → 08:00 ... 17:00 = 37 mốc, mỗi mốc 2 nhân viên
        assertThat(slots).hasSize(37);
        assertThat(slots.get(0).get("staffIds")).hasSize(2);
        assertThat(fetch("/api/public/businesses/" + s.slug() + "/staff?serviceId=" + s.serviceId() + "&branchId=" + s.branchId(), null)).hasSize(2);
        call(get("/api/public/businesses/khong-ton-tai"), null, null).andExpect(status().isNotFound());
    }

    @Test
    void bookingLocksSlotAndPreventsOverlap() throws Exception {
        Shop s = createShop();
        book(s, s.staffA(), TOMORROW, "10:00", "0912345678").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("BE[A-Z0-9]{6}")));
        // trùng giờ & chồng lấn một phần với cùng nhân viên
        book(s, s.staffA(), TOMORROW, "10:00", "0912000001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
        book(s, s.staffA(), TOMORROW, "10:30", "0912000002").andExpect(status().isConflict());
        book(s, s.staffA(), TOMORROW, "09:15", "0912000003").andExpect(status().isConflict());
        // ngay sau đó thì được
        book(s, s.staffA(), TOMORROW, "11:00", "0912000004").andExpect(status().isCreated());
        JsonNode aSlots = slots(s, s.staffA());
        assertThat(hasTime(aSlots, "10:00")).isFalse();
        assertThat(hasTime(aSlots, "09:30")).isFalse();
        assertThat(hasTime(aSlots, "12:00")).isTrue();
    }

    @Test
    void anyStaffAssignsAnotherFreeStaff() throws Exception {
        Shop s = createShop();
        JsonNode first = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, null, TOMORROW, "14:00", "0912345678"));
        JsonNode second = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, null, TOMORROW, "14:00", "0987654321"));
        assertThat(first.get("staffName").asText()).isNotEqualTo(second.get("staffName").asText());
        book(s, null, TOMORROW, "14:00", "0911111111").andExpect(status().isConflict());
    }

    @Test
    void rejectsOutsideHoursPastAndInvalidInput() throws Exception {
        Shop s = createShop();
        book(s, s.staffA(), TOMORROW, "19:00", "0912345678").andExpect(status().isConflict());
        book(s, s.staffA(), NOW.toLocalDate(), "08:30", "0912345678").andExpect(status().isConflict()); // trước min lead 60'
        book(s, s.staffA(), TOMORROW, "10:10", "0912345678").andExpect(status().isBadRequest());
        book(s, s.staffA(), TOMORROW, "10:00", "12345").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
        call(post("/api/public/businesses/" + s.slug() + "/bookings"), null, Map.of("branchId", s.branchId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.serviceId").exists());
    }

    @Test
    void holidaysAndTimeOffRemoveAvailability() throws Exception {
        Shop s = createShop();
        ok(post("/api/staff/" + s.staffA() + "/time-off"), s.ownerToken(), Map.of("startDate", TOMORROW.toString(), "endDate", TOMORROW.toString()));
        assertThat(slots(s, s.staffA())).isEmpty();
        assertThat(slots(s, null).get(0).get("staffIds")).hasSize(1);
        ok(post("/api/holidays"), s.ownerToken(), Map.of("date", TOMORROW.toString(), "name", "Nghỉ lễ"));
        assertThat(slots(s, null)).isEmpty();
    }

    @Test
    void customerLookupAndCancelReleasesSlot() throws Exception {
        Shop s = createShop();
        String code = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), TOMORROW, "10:00", "0912 345 678"))
                .get("code").asText();
        call(get("/api/public/bookings/" + code + "?phone=0999999999"), null, null).andExpect(status().isNotFound());
        JsonNode found = fetch("/api/public/bookings/" + code + "?phone=84912345678", null);
        assertThat(found.get("cancellable").asBoolean()).isTrue();
        ok(post("/api/public/bookings/" + code + "/cancel"), null, Map.of("phone", "0912345678"));
        assertThat(fetch("/api/public/bookings/" + code + "?phone=0912345678", null).get("status").asText()).isEqualTo("CANCELLED");
        book(s, s.staffA(), TOMORROW, "10:00", "0987654321").andExpect(status().isCreated());
    }

    @Test
    void ownerManagesStatusFlowRescheduleAndReport() throws Exception {
        Shop s = createShop();
        String t = s.ownerToken();
        ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), TOMORROW, "10:00", "0912345678"));
        ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffB(), TOMORROW, "11:00", "0987654321"));
        JsonNode list = fetch("/api/bookings?from=" + TOMORROW + "T00:00:00&to=" + TOMORROW.plusDays(1) + "T00:00:00", t);
        assertThat(list).hasSize(2);
        long id1 = list.get(0).get("id").asLong();
        long id2 = list.get(1).get("id").asLong();

        // đổi sang giờ của booking kia (cùng nhân viên B) → 409; sang giờ trống → ok
        call(patch("/api/bookings/" + id1 + "/reschedule"), t, Map.of("startAt", TOMORROW + "T11:30:00", "staffId", s.staffB()))
                .andExpect(status().isConflict());
        JsonNode moved = ok(patch("/api/bookings/" + id1 + "/reschedule"), t, Map.of("startAt", TOMORROW + "T13:00:00", "staffId", s.staffB()));
        assertThat(moved.get("staff").get("id").asLong()).isEqualTo(s.staffB());
        // giờ cũ của A được giải phóng
        book(s, s.staffA(), TOMORROW, "10:00", "0911111111").andExpect(status().isCreated());

        // luồng trạng thái
        call(post("/api/bookings/" + id1 + "/status"), t, Map.of("status", "COMPLETED")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        call(post("/api/bookings/" + id1 + "/status"), t, Map.of("status", "NO_SHOW")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_EARLY"));
        ok(post("/api/bookings/" + id1 + "/status"), t, Map.of("status", "CHECKED_IN"));
        ok(post("/api/bookings/" + id1 + "/status"), t, Map.of("status", "COMPLETED"));
        clock.set(TOMORROW.atTime(12, 30));
        t = login(s.slug() + "@test.vn", "Matkhau123"); // access token cũ đã hết hạn sau khi tua giờ
        ok(post("/api/bookings/" + id2 + "/status"), t, Map.of("status", "NO_SHOW"));

        JsonNode report = fetch("/api/reports/summary?from=" + TOMORROW + "&to=" + TOMORROW, t);
        assertThat(report.get("revenue").asLong()).isEqualTo(300000);
        assertThat(report.get("completed").asInt()).isEqualTo(1);
        assertThat(report.get("noShow").asInt()).isEqualTo(1);
        assertThat(report.get("noShowRate").asDouble()).isEqualTo(0.5);
        assertThat(report.get("topServices").get(0).get("count").asInt()).isEqualTo(1);

        JsonNode customers = fetch("/api/customers", t);
        assertThat(customers.get("totalElements").asInt()).isEqualTo(3);
    }

    @Test
    void depositFlowViaWebhookIsIdempotentAndExpiresUnpaid() throws Exception {
        Shop s = createShop();
        JsonNode biz = fetch("/api/business", s.ownerToken());
        Map<String, Object> upd = json.convertValue(biz, Map.class);
        upd.put("paymentMode", "DEPOSIT");
        upd.put("depositPercent", 30);
        upd.put("bankBin", "970436");
        upd.put("bankAccountNo", "1023456789");
        upd.put("bankAccountName", "spa thu nghiem");
        ok(put("/api/business"), s.ownerToken(), upd);

        JsonNode b = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), TOMORROW, "10:00", "0912345678"));
        assertThat(b.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(b.get("amountDue").asLong()).isEqualTo(90000);
        assertThat(b.get("payment").get("qrUrl").asText()).contains("970436-1023456789").contains("amount=90000").contains("addInfo=" + b.get("code").asText());

        String payload = "{\"id\":555,\"transferType\":\"in\",\"transferAmount\":90000,\"content\":\"CK " + b.get("code").asText().toLowerCase() + "\"}";
        mvc.perform(post("/api/webhooks/payments/sepay").header("Authorization", "Apikey wrong").contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/payments/sepay").header("Authorization", "Apikey dev-webhook-secret").contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.APPLIED").value(1));
        mvc.perform(post("/api/webhooks/payments/sepay").header("Authorization", "Apikey dev-webhook-secret").contentType("application/json").content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.DUPLICATE").value(1));
        JsonNode after = fetch("/api/public/bookings/" + b.get("code").asText() + "?phone=0912345678", null);
        assertThat(after.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(after.get("paymentStatus").asText()).isEqualTo("PAID");

        // booking thứ hai không thanh toán → quá 30 phút bị hủy, slot mở lại
        JsonNode unpaid = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), TOMORROW, "13:00", "0987654321"));
        clock.advance(Duration.ofMinutes(31));
        scheduler.expirePendingPayments();
        assertThat(fetch("/api/public/bookings/" + unpaid.get("code").asText() + "?phone=0987654321", null).get("status").asText()).isEqualTo("CANCELLED");
        book(s, s.staffA(), TOMORROW, "13:00", "0911111111").andExpect(status().isCreated());
    }

    @Test
    void remindersAreSentOncePerWindow() throws Exception {
        Shop s = createShop();
        // đặt trước 3 ngày → nhắc 24h và 2h đều áp dụng
        JsonNode b = ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), NOW.toLocalDate().plusDays(3), "10:00", "0912345678"));
        Booking bk = bookings.findByCodeDetailed(b.get("code").asText()).orElseThrow();
        clock.set(bk.getStartAt().minusHours(23));
        scheduler.sendReminders();
        scheduler.sendReminders();
        clock.set(bk.getStartAt().minusMinutes(90));
        scheduler.sendReminders();
        Booking reloaded = bookings.findById(bk.getId()).orElseThrow();
        assertThat(reloaded.getReminder24hSentAt()).isNotNull();
        assertThat(reloaded.getReminder2hSentAt()).isNotNull();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var logs = notificationLogs.findByBookingIdOrderByCreatedAtDesc(bk.getId());
            assertThat(logs).filteredOn(l -> l.getType().equals("REMINDER_24H") && l.getChannel().equals("ZALO_ZNS")).hasSize(1);
            assertThat(logs).filteredOn(l -> l.getType().equals("REMINDER_2H") && l.getChannel().equals("ZALO_ZNS")).hasSize(1);
            assertThat(logs).filteredOn(l -> l.getType().equals("CONFIRMATION")).isNotEmpty();
        });
    }

    @Test
    void staffSeesOnlyOwnBookingsAndLimitedActions() throws Exception {
        Shop s = createShop();
        ok(post("/api/staff/" + s.staffA() + "/account"), s.ownerToken(), Map.of("email", "a-" + s.slug() + "@test.vn", "password", "Matkhau123"));
        String staffToken = login("a-" + s.slug() + "@test.vn", "Matkhau123");
        ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffA(), TOMORROW, "10:00", "0912345678"));
        ok(post("/api/public/businesses/" + s.slug() + "/bookings"), null, publicBooking(s, s.staffB(), TOMORROW, "10:00", "0987654321"));
        JsonNode mine = fetch("/api/bookings?from=" + TOMORROW + "T00:00:00&to=" + TOMORROW.plusDays(1) + "T00:00:00&staffId=" + s.staffB(), staffToken);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("staff").get("id").asLong()).isEqualTo(s.staffA());

        JsonNode all = fetch("/api/bookings?from=" + TOMORROW + "T00:00:00&to=" + TOMORROW.plusDays(1) + "T00:00:00", s.ownerToken());
        long othersId = StreamSupport.stream(all.spliterator(), false).filter(n -> n.get("staff").get("id").asLong() == s.staffB()).findFirst().orElseThrow().get("id").asLong();
        call(get("/api/bookings/" + othersId), staffToken, null).andExpect(status().isNotFound());
        call(post("/api/bookings/" + mine.get(0).get("id").asLong() + "/status"), staffToken, Map.of("status", "CANCELLED")).andExpect(status().isForbidden());
        ok(post("/api/bookings/" + mine.get(0).get("id").asLong() + "/status"), staffToken, Map.of("status", "CHECKED_IN"));
        call(get("/api/customers"), staffToken, null).andExpect(status().isForbidden());
        call(delete("/api/services/" + s.serviceId()), staffToken, null).andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesWalkInBookingWithConflictCheck() throws Exception {
        Shop s = createShop();
        Map<String, Object> req = Map.of("serviceId", s.serviceId(), "staffId", s.staffA(), "startAt", TOMORROW + "T09:00:00",
                "fullName", "Khách Vãng Lai", "phone", "0901234567");
        JsonNode b = ok(post("/api/bookings"), s.ownerToken(), req);
        assertThat(b.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(b.get("source").asText()).isEqualTo("ADMIN");
        call(post("/api/bookings"), s.ownerToken(), req).andExpect(status().isConflict());
    }
}
