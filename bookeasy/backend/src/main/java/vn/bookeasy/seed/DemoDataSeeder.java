package vn.bookeasy.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.auth.Role;
import vn.bookeasy.auth.User;
import vn.bookeasy.auth.UserRepository;
import vn.bookeasy.booking.*;
import vn.bookeasy.business.*;
import vn.bookeasy.catalog.*;
import vn.bookeasy.customer.Customer;
import vn.bookeasy.customer.CustomerRepository;
import vn.bookeasy.staff.*;

import java.time.*;
import java.util.*;

/** Dữ liệu demo tiếng Việt: 3 cơ sở, tài khoản cho từng vai trò, lịch hẹn 30 ngày qua và 7 ngày tới. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    public static final String DEMO_PASSWORD = "Demo@123";

    private final BusinessRepository businesses;
    private final BranchRepository branches;
    private final BranchHourRepository hours;
    private final ServiceCategoryRepository categories;
    private final ServiceItemRepository services;
    private final StaffRepository staffRepo;
    private final StaffShiftRepository shifts;
    private final StaffTimeOffRepository timeOffs;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;
    private final HolidayRepository holidays;
    private final BookingCodeGenerator codes;
    private final PasswordEncoder encoder;
    private final Clock clock;

    private final Random random = new Random(42);
    private String hash;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (businesses.count() > 0) return;
        log.info("Seeding demo data...");
        hash = encoder.encode(DEMO_PASSWORD);
        user(null, null, "admin@bookeasy.vn", "Quản trị BookEasy", Role.SUPER_ADMIN);
        seedSpa();
        seedGym();
        seedClinic();
        log.info("Demo data ready. Password for all demo accounts: {}", DEMO_PASSWORD);
    }

    private void seedSpa() {
        Business b = business("lotus-spa", "Lotus Spa & Massage", BusinessCategory.SPA, "#2f7d6b",
                "Không gian thư giãn giữa lòng Sài Gòn. Massage trị liệu, chăm sóc da và gội đầu dưỡng sinh bằng thảo mộc Việt.",
                "0283 930 1234", "48 Trần Quốc Thảo, Phường Võ Thị Sáu, Quận 3, TP.HCM");
        b.setPaymentMode(PaymentMode.DEPOSIT);
        b.setDepositPercent(30);
        b.setBankBin("970436");
        b.setBankAccountNo("1023456789");
        b.setBankAccountName("CONG TY TNHH LOTUS SPA");
        Branch q3 = branch(b, "Lotus Spa Quận 3", "48 Trần Quốc Thảo, Phường Võ Thị Sáu, Quận 3, TP.HCM", "0283 930 1234", LocalTime.of(9, 0), LocalTime.of(21, 0));
        Branch q7 = branch(b, "Lotus Spa Phú Mỹ Hưng", "102 Nguyễn Đức Cảnh, Tân Phong, Quận 7, TP.HCM", "0283 541 5678", LocalTime.of(9, 0), LocalTime.of(21, 30));
        ServiceCategory massage = category(b, "Massage trị liệu", 1);
        ServiceCategory skin = category(b, "Chăm sóc da mặt", 2);
        ServiceCategory hair = category(b, "Gội đầu dưỡng sinh", 3);
        Staff lan = staff(b, q3, "Nguyễn Thị Lan", "Kỹ thuật viên trưởng", "#2f7d6b", "0901 234 567");
        Staff mai = staff(b, q3, "Trần Ngọc Mai", "Chuyên viên da", "#b4573a", "0902 345 678");
        Staff hoa = staff(b, q3, "Lê Thị Hoa", "Kỹ thuật viên", "#5a6fb0", "0903 456 789");
        Staff thu = staff(b, q7, "Phạm Minh Thư", "Kỹ thuật viên", "#8a5bb0", "0904 567 890");
        Staff vy = staff(b, q7, "Đỗ Tường Vy", "Chuyên viên da", "#b08a2f", "0905 678 901");
        for (Staff s : List.of(lan, mai, hoa, thu, vy)) shiftsSplit(s);
        timeOff(hoa, LocalDate.now(clock).plusDays(3), LocalDate.now(clock).plusDays(4), "Nghỉ phép");
        List<ServiceItem> svcs = List.of(
                service(b, massage, "Massage body đá nóng", "Đá bazan nóng kết hợp tinh dầu sả chanh, giảm đau mỏi vai gáy.", 90, 450_000, lan, hoa, thu),
                service(b, massage, "Massage chân thảo dược", "Ngâm chân thảo mộc và bấm huyệt bàn chân.", 60, 280_000, lan, hoa, thu, vy),
                service(b, massage, "Massage Thái cổ truyền", "Kéo giãn cơ, đả thông kinh lạc theo phương pháp Thái.", 75, 380_000, lan, thu),
                service(b, skin, "Chăm sóc da cơ bản", "Làm sạch sâu, tẩy tế bào chết, đắp mặt nạ dưỡng ẩm.", 60, 350_000, mai, vy),
                service(b, skin, "Trị mụn chuyên sâu", "Lấy nhân mụn chuẩn y khoa, điện di tinh chất phục hồi.", 90, 550_000, mai, vy),
                service(b, skin, "Nâng cơ trẻ hóa da", "Công nghệ RF nâng cơ, giảm nếp nhăn vùng mắt.", 75, 890_000, mai),
                service(b, hair, "Gội đầu dưỡng sinh", "Gội thảo dược bồ kết, massage cổ vai gáy 20 phút.", 45, 150_000, lan, hoa, thu, vy),
                service(b, hair, "Gội đầu + đắp mặt nạ", "Gội dưỡng sinh kèm đắp mặt nạ collagen.", 60, 220_000, hoa, thu, vy));
        user(b, null, "owner@lotusspa.vn", "Võ Thị Hạnh", Role.OWNER);
        user(b, lan, "lan@lotusspa.vn", lan.getFullName(), Role.STAFF);
        user(b, thu, "thu@lotusspa.vn", thu.getFullName(), Role.STAFF);
        holiday(b, LocalDate.of(LocalDate.now(clock).getYear() + 1, 1, 1), "Tết Dương lịch");
        bookingsFor(b, List.of(q3, q7), svcs, 4);
    }

    private void seedGym() {
        Business b = business("titan-fitness", "Titan Fitness", BusinessCategory.GYM, "#3f5fa8",
                "Phòng tập chuẩn quốc tế với huấn luyện viên cá nhân, lớp Yoga và đo chỉ số InBody miễn phí cho hội viên.",
                "0243 777 8899", "215 Xuân Thủy, Dịch Vọng Hậu, Cầu Giấy, Hà Nội");
        Branch main = branch(b, "Titan Cầu Giấy", "215 Xuân Thủy, Dịch Vọng Hậu, Cầu Giấy, Hà Nội", "0243 777 8899", LocalTime.of(6, 0), LocalTime.of(22, 0));
        ServiceCategory pt = category(b, "Huấn luyện cá nhân", 1);
        ServiceCategory cls = category(b, "Lớp nhóm nhỏ", 2);
        Staff hung = staff(b, main, "Trần Quốc Hùng", "PT cao cấp", "#3f5fa8", "0912 111 222");
        Staff duc = staff(b, main, "Nguyễn Minh Đức", "PT", "#2f7d6b", "0912 333 444");
        Staff linh = staff(b, main, "Hoàng Khánh Linh", "HLV Yoga", "#b4573a", "0912 555 666");
        for (Staff s : List.of(hung, duc, linh)) {
            for (int d = 1; d <= 6; d++) {
                shift(s, d, LocalTime.of(6, 0), LocalTime.of(11, 0));
                shift(s, d, LocalTime.of(16, 0), LocalTime.of(21, 0));
            }
        }
        List<ServiceItem> svcs = List.of(
                service(b, pt, "PT 1 kèm 1 – 60 phút", "Giáo án cá nhân hóa theo mục tiêu giảm mỡ/tăng cơ.", 60, 400_000, hung, duc),
                service(b, pt, "Đo InBody & tư vấn dinh dưỡng", "Phân tích thành phần cơ thể và lên thực đơn.", 30, 0, hung, duc, linh),
                service(b, cls, "Yoga trị liệu cột sống", "Lớp tối đa 1 học viên/HLV, phù hợp dân văn phòng.", 60, 250_000, linh),
                service(b, cls, "Boxing cơ bản", "Kỹ thuật đấm, di chuyển và thể lực.", 45, 220_000, duc));
        user(b, null, "owner@titanfitness.vn", "Trần Văn Nam", Role.OWNER);
        user(b, hung, "hung@titanfitness.vn", hung.getFullName(), Role.STAFF);
        bookingsFor(b, List.of(main), svcs, 3);
    }

    private void seedClinic() {
        Business b = business("nha-khoa-an-tam", "Nha khoa An Tâm", BusinessCategory.CLINIC, "#1f7a8c",
                "Phòng khám nha khoa gia đình. Bác sĩ giàu kinh nghiệm, trang thiết bị hiện đại, vô trùng tuyệt đối.",
                "0236 382 4567", "120 Nguyễn Văn Linh, Nam Dương, Hải Châu, Đà Nẵng");
        Branch main = branch(b, "An Tâm Hải Châu", "120 Nguyễn Văn Linh, Nam Dương, Hải Châu, Đà Nẵng", "0236 382 4567", LocalTime.of(8, 0), LocalTime.of(20, 0));
        ServiceCategory gen = category(b, "Nha khoa tổng quát", 1);
        ServiceCategory cos = category(b, "Thẩm mỹ", 2);
        Staff an = staff(b, main, "BS. Lê Hoàng An", "Bác sĩ RHM", "#1f7a8c", "0935 123 456");
        Staff trang = staff(b, main, "BS. Phan Thu Trang", "Bác sĩ chỉnh nha", "#8a5bb0", "0935 654 321");
        for (Staff s : List.of(an, trang)) {
            for (int d = 1; d <= 6; d++) {
                shift(s, d, LocalTime.of(8, 0), LocalTime.of(12, 0));
                shift(s, d, LocalTime.of(13, 30), LocalTime.of(19, 30));
            }
        }
        List<ServiceItem> svcs = List.of(
                service(b, gen, "Khám & tư vấn", "Khám tổng quát, chụp phim nếu cần.", 30, 100_000, an, trang),
                service(b, gen, "Cạo vôi răng & đánh bóng", "Làm sạch mảng bám, đánh bóng bằng máy.", 45, 300_000, an, trang),
                service(b, gen, "Trám răng thẩm mỹ", "Trám composite màu răng tự nhiên.", 45, 400_000, an),
                service(b, cos, "Tẩy trắng răng tại phòng khám", "Công nghệ Laser Whitening, trắng 2–4 tông.", 90, 2_500_000, trang));
        user(b, null, "owner@nhakhoaantam.vn", "BS. Lê Hoàng An", Role.OWNER);
        bookingsFor(b, List.of(main), svcs, 3);
    }

    // ---------------------------------------------------------------------------------

    private static final String[] LAST = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương"};
    private static final String[] MIDDLE = {"Thị", "Văn", "Minh", "Ngọc", "Thanh", "Hoàng", "Gia", "Bảo", "Thu", "Quốc"};
    private static final String[] FIRST = {"Anh", "Bình", "Châu", "Dung", "Giang", "Hà", "Hải", "Hằng", "Hiếu", "Hương", "Khoa", "Linh", "Long", "My", "Nam", "Ngân", "Nhung", "Phúc", "Quân", "Quỳnh", "Sơn", "Tâm", "Thảo", "Trâm", "Trung", "Tú", "Uyên", "Vân", "Yến", "Khánh"};
    private static final String[] PREFIX = {"090", "091", "093", "094", "097", "098", "032", "033", "035", "038", "070", "077", "079", "081", "085"};

    private void bookingsFor(Business b, List<Branch> brs, List<ServiceItem> svcs, int maxPerStaffPerDay) {
        List<Customer> pool = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            Customer c = new Customer();
            c.setBusinessId(b.getId());
            c.setFullName(LAST[random.nextInt(LAST.length)] + " " + MIDDLE[random.nextInt(MIDDLE.length)] + " " + FIRST[random.nextInt(FIRST.length)]);
            c.setPhone(PREFIX[random.nextInt(PREFIX.length)] + String.format("%07d", random.nextInt(10_000_000)));
            if (customers.findByBusinessIdAndPhone(b.getId(), c.getPhone()).isPresent()) continue;
            if (random.nextInt(3) > 0) c.setEmail("khach" + (i + 1) + "@example.com");
            if (i % 9 == 0) c.setNotes("Khách quen, thích phòng yên tĩnh. Dị ứng tinh dầu bạc hà.");
            c.setCreatedAt(LocalDateTime.now(clock).minusDays(60 - i));
            pool.add(customers.save(c));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        for (int offset = -30; offset <= 7; offset++) {
            LocalDate day = today.plusDays(offset);
            int dow = day.getDayOfWeek().getValue();
            Set<Staff> allStaff = new LinkedHashSet<>();
            svcs.forEach(s -> allStaff.addAll(s.getStaff()));
            for (Staff st : allStaff) {
                List<ServiceItem> canDo = svcs.stream().filter(s -> s.getStaff().contains(st)).toList();
                List<StaffShift> sh = shifts.findByStaffIdInAndDayOfWeek(List.of(st.getId()), dow);
                if (sh.isEmpty() || canDo.isEmpty()) continue;
                int n = random.nextInt(maxPerStaffPerDay + 1);
                Set<LocalDateTime> busy = new HashSet<>();
                for (int k = 0; k < n * 3 && n > 0; k++) {
                    StaffShift s = sh.get(random.nextInt(sh.size()));
                    ServiceItem svc = canDo.get(random.nextInt(canDo.size()));
                    int slotsInShift = (int) (Duration.between(s.getStartTime(), s.getEndTime()).toMinutes() - svc.getDurationMinutes()) / 15;
                    if (slotsInShift < 0) continue;
                    LocalDateTime start = day.atTime(s.getStartTime()).plusMinutes(15L * random.nextInt(slotsInShift + 1));
                    if (!SlotCalculator.isFree(start, svc.getDurationMinutes(), busy)) continue;
                    if (offset > 0 && start.isBefore(now)) continue;
                    Branch br = brs.stream().filter(x -> x.getId().equals(st.getBranchId())).findFirst().orElse(brs.get(0));
                    BookingStatus status = statusFor(start, now, svc.getDurationMinutes());
                    Booking bk = booking(b, br, svc, st, pool.get(random.nextInt(pool.size())), start, status);
                    if (status != BookingStatus.CANCELLED) {
                        List<LocalDateTime> units = SlotCalculator.units(start, svc.getDurationMinutes());
                        busy.addAll(units);
                        for (LocalDateTime u : units) slots.save(new BookingSlot(bk.getId(), st.getId(), u));
                    }
                    if (--n <= 0) break;
                }
            }
        }
    }

    private BookingStatus statusFor(LocalDateTime start, LocalDateTime now, int duration) {
        if (start.plusMinutes(duration).isBefore(now)) {
            int r = random.nextInt(100);
            return r < 80 ? BookingStatus.COMPLETED : r < 88 ? BookingStatus.NO_SHOW : BookingStatus.CANCELLED;
        }
        if (start.isBefore(now)) return BookingStatus.CHECKED_IN;
        return random.nextInt(100) < 92 ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED;
    }

    private Booking booking(Business b, Branch br, ServiceItem svc, Staff st, Customer c, LocalDateTime start, BookingStatus status) {
        Booking bk = new Booking();
        bk.setBusinessId(b.getId());
        bk.setCode(codes.next());
        bk.setBranch(br);
        bk.setService(svc);
        bk.setStaff(st);
        bk.setCustomer(c);
        bk.setStartAt(start);
        bk.setEndAt(start.plusMinutes(svc.getDurationMinutes()));
        bk.setStatus(status);
        bk.setPrice(svc.getPrice());
        bk.setSource(random.nextInt(10) < 7 ? BookingSource.ONLINE : BookingSource.ADMIN);
        long due = b.paymentConfigured() && bk.getSource() == BookingSource.ONLINE ? Math.min(svc.getPrice(), ((svc.getPrice() * b.getDepositPercent() / 100 + 999) / 1000) * 1000) : 0;
        if (due > 0) {
            bk.setAmountDue(due);
            bk.setAmountPaid(due);
            bk.setPaymentStatus(PaymentStatus.PAID);
        }
        bk.setCreatedAt(start.minusDays(1 + random.nextInt(5)));
        if (start.isBefore(LocalDateTime.now(clock).plusHours(24))) bk.setReminder24hSentAt(start.minusHours(24));
        if (start.isBefore(LocalDateTime.now(clock).plusHours(2))) bk.setReminder2hSentAt(start.minusHours(2));
        if (status == BookingStatus.CANCELLED) {
            bk.setCancelReason(random.nextBoolean() ? "Khách bận đột xuất" : "Khách đổi lịch khác");
            bk.setCancelledAt(start.minusHours(5));
        }
        if (random.nextInt(8) == 0) bk.setCustomerNote("Cho mình phòng riêng nếu được ạ");
        return bookings.save(bk);
    }

    private Business business(String slug, String name, BusinessCategory cat, String color, String desc, String phone, String address) {
        Business b = new Business();
        b.setSlug(slug);
        b.setName(name);
        b.setCategory(cat);
        b.setBrandColor(color);
        b.setDescription(desc);
        b.setPhone(phone);
        b.setAddress(address);
        b.setMinLeadMinutes(60);
        return businesses.save(b);
    }

    private Branch branch(Business b, String name, String address, String phone, LocalTime open, LocalTime close) {
        Branch br = new Branch();
        br.setBusinessId(b.getId());
        br.setName(name);
        br.setAddress(address);
        br.setPhone(phone);
        branches.save(br);
        for (int d = 1; d <= 7; d++) {
            BranchHour h = new BranchHour();
            h.setBranchId(br.getId());
            h.setDayOfWeek(d);
            h.setOpenTime(open);
            h.setCloseTime(close);
            hours.save(h);
        }
        return br;
    }

    private ServiceCategory category(Business b, String name, int order) {
        ServiceCategory c = new ServiceCategory();
        c.setBusinessId(b.getId());
        c.setName(name);
        c.setSortOrder(order);
        return categories.save(c);
    }

    private Staff staff(Business b, Branch br, String name, String title, String color, String phone) {
        Staff s = new Staff();
        s.setBusinessId(b.getId());
        s.setBranchId(br.getId());
        s.setFullName(name);
        s.setTitle(title);
        s.setColor(color);
        s.setPhone(phone);
        return staffRepo.save(s);
    }

    private void shiftsSplit(Staff s) {
        for (int d = 1; d <= 7; d++) {
            if ((s.getId() + d) % 7 == 0) continue; // mỗi người nghỉ 1 ngày/tuần
            shift(s, d, LocalTime.of(9, 0), LocalTime.of(13, 0));
            shift(s, d, LocalTime.of(14, 0), LocalTime.of(21, 0));
        }
    }

    private void shift(Staff s, int dow, LocalTime start, LocalTime end) {
        StaffShift sh = new StaffShift();
        sh.setStaffId(s.getId());
        sh.setDayOfWeek(dow);
        sh.setStartTime(start);
        sh.setEndTime(end);
        shifts.save(sh);
    }

    private void timeOff(Staff s, LocalDate from, LocalDate to, String reason) {
        StaffTimeOff t = new StaffTimeOff();
        t.setStaffId(s.getId());
        t.setStartDate(from);
        t.setEndDate(to);
        t.setReason(reason);
        timeOffs.save(t);
    }

    private void holiday(Business b, LocalDate date, String name) {
        Holiday h = new Holiday();
        h.setBusinessId(b.getId());
        h.setHolidayDate(date);
        h.setName(name);
        holidays.save(h);
    }

    private ServiceItem service(Business b, ServiceCategory c, String name, String desc, int duration, long price, Staff... staff) {
        ServiceItem s = new ServiceItem();
        s.setBusinessId(b.getId());
        s.setCategoryId(c.getId());
        s.setName(name);
        s.setDescription(desc);
        s.setDurationMinutes(duration);
        s.setPrice(price);
        s.getStaff().addAll(Arrays.asList(staff));
        return services.save(s);
    }

    private void user(Business b, Staff staff, String email, String name, Role role) {
        User u = new User();
        u.setBusinessId(b == null ? null : b.getId());
        u.setStaffId(staff == null ? null : staff.getId());
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setPasswordHash(hash);
        users.save(u);
    }
}
