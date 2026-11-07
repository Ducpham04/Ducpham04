package vn.educenter.seed;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.educenter.attendance.Attendance;
import vn.educenter.attendance.AttendanceRepository;
import vn.educenter.billing.*;
import vn.educenter.catalog.CatalogDtos;
import vn.educenter.catalog.CatalogService;
import vn.educenter.classes.*;
import vn.educenter.config.TimeConfig;
import vn.educenter.security.AuthUser;
import vn.educenter.security.Role;
import vn.educenter.student.Student;
import vn.educenter.student.StudentDtos;
import vn.educenter.student.StudentRepository;
import vn.educenter.student.StudentService;
import vn.educenter.tenant.Tenant;
import vn.educenter.tenant.TenantRepository;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;
import vn.educenter.user.UserService;

/**
 * Dữ liệu demo tiếng Việt (bật bằng APP_SEED_DEMO=true, chỉ chạy khi DB trống).
 * Ngày tháng tính tương đối theo hôm nay để dashboard, lịch, nhắc nhở luôn có dữ liệu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo@123";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final UserService userService;
    private final StudentService studentService;
    private final CatalogService catalogService;
    private final ClassService classService;
    private final ClassSessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final BillingService billingService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate tx;
    private final Clock clock;

    private final Random random = new Random(2026);

    @Override
    public void run(ApplicationArguments args) {
        if (tenantRepository.count() > 0) {
            log.info("Bỏ qua seed demo: database đã có dữ liệu");
            return;
        }
        log.info("Đang tạo dữ liệu demo...");
        try {
            tx.executeWithoutResult(s -> {
                User superAdmin = userService.newUser(null, "admin@educenter.vn", "Admin@123", "Quản trị EduCenter", Role.SUPER_ADMIN);
                userRepository.save(superAdmin);
            });
            Tenant hoaSen = tx.execute(s -> createTenant("Anh ngữ Hoa Sen", "hoasen", "02873001234", "lienhe@hoasen.edu.vn",
                    "125 Nguyễn Thị Minh Khai, Phường Bến Thành, TP. Hồ Chí Minh", Tenant.Plan.PRO, "VCB", "1023456789", "TRUNG TAM ANH NGU HOA SEN"));
            seedHoaSen(hoaSen);
            Tenant saoMai = tx.execute(s -> createTenant("Ngoại ngữ Sao Mai", "saomai", "02363888999", "hotro@saomai.edu.vn",
                    "48 Lê Duẩn, Hải Châu, Đà Nẵng", Tenant.Plan.BASIC, "TCB", "19036688990011", "NGOAI NGU SAO MAI"));
            seedSaoMai(saoMai);
            log.info("Đã tạo dữ liệu demo. Mật khẩu chung: {}", DEMO_PASSWORD);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Tenant createTenant(String name, String slug, String phone, String email, String address, Tenant.Plan plan,
                                String bank, String accNo, String accName) {
        Tenant t = new Tenant();
        t.setName(name);
        t.setSlug(slug);
        t.setPhone(phone);
        t.setEmail(email);
        t.setAddress(address);
        t.setPlan(plan);
        t.setStatus(Tenant.Status.ACTIVE);
        t.setBankCode(bank);
        t.setBankAccountNo(accNo);
        t.setBankAccountName(accName);
        return tenantRepository.save(t);
    }

    private User user(Long tenantId, String email, String name, String phone, Role role) {
        User u = userService.newUser(tenantId, email, DEMO_PASSWORD, name, role);
        u.setPhone(phone);
        return userRepository.save(u);
    }

    private void actAs(User u) {
        AuthUser principal = new AuthUser(u.getId(), u.getTenantId(), u.getRole(), u.getStudentId(), u.getEmail());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
    }

    // ------------------------------------------------------------------ Hoa Sen

    private void seedHoaSen(Tenant t) {
        LocalDate today = LocalDate.now(clock);
        Map<String, User> users = tx.execute(s -> {
            Map<String, User> m = new LinkedHashMap<>();
            m.put("admin", user(t.getId(), "chulan@hoasen.edu.vn", "Nguyễn Thị Lan", "0903123456", Role.CENTER_ADMIN));
            m.put("staff", user(t.getId(), "giaovu@hoasen.edu.vn", "Trần Minh Thu", "0938765432", Role.STAFF));
            m.put("david", user(t.getId(), "david@hoasen.edu.vn", "David Nguyễn", "0912000111", Role.TEACHER));
            m.put("hanh", user(t.getId(), "hanh@hoasen.edu.vn", "Lê Thị Hạnh", "0912000222", Role.TEACHER));
            m.put("quang", user(t.getId(), "quang@hoasen.edu.vn", "Phạm Văn Quang", "0912000333", Role.TEACHER));
            return m;
        });
        actAs(users.get("staff"));

        Map<String, Long> rooms = new HashMap<>();
        Map<String, CatalogDtos.CourseResponse> courses = new LinkedHashMap<>();
        tx.executeWithoutResult(s -> {
            rooms.put("P101", catalogService.createRoom(new CatalogDtos.RoomRequest("P101", 20, true)).id());
            rooms.put("P102", catalogService.createRoom(new CatalogDtos.RoomRequest("P102", 16, true)).id());
            rooms.put("P201", catalogService.createRoom(new CatalogDtos.RoomRequest("P201", 24, true)).id());
            rooms.put("P202", catalogService.createRoom(new CatalogDtos.RoomRequest("P202 – Phòng nhỏ", 10, true)).id());
            courses.put("IELTS-F", catalogService.createCourse(new CatalogDtos.CourseRequest("IELTS-F", "IELTS Foundation (0 – 4.5)", "A2–B1", 36, new BigDecimal("7200000"), "Nền tảng 4 kỹ năng, làm quen dạng đề IELTS.", true)));
            courses.put("IELTS-65", catalogService.createCourse(new CatalogDtos.CourseRequest("IELTS-65", "IELTS Intensive 6.5+", "B2", 40, new BigDecimal("9800000"), "Luyện đề chuyên sâu, chữa Writing/Speaking 1-1.", true)));
            courses.put("TOEIC-650", catalogService.createCourse(new CatalogDtos.CourseRequest("TOEIC-650", "TOEIC 650+", "B1", 30, new BigDecimal("5400000"), "Cam kết đầu ra 650 TOEIC Listening & Reading.", true)));
            courses.put("KIDS-ST", catalogService.createCourse(new CatalogDtos.CourseRequest("KIDS-ST", "Tiếng Anh thiếu nhi – Starters", "Pre-A1", 48, new BigDecimal("4800000"), "Cho bé 6–8 tuổi, giáo trình Cambridge Starters.", true)));
            courses.put("GT-PX", catalogService.createCourse(new CatalogDtos.CourseRequest("GT-PX", "Giao tiếp phản xạ cho người đi làm", "A2–B1", 24, new BigDecimal("4200000"), "Lớp tối, 100% nói, chủ đề công sở.", true)));
        });

        Map<String, BillingDtos.FeePlanResponse> plans = new LinkedHashMap<>();
        tx.executeWithoutResult(s -> {
            plans.put("IELTS-F", billingService.createFeePlan(new BillingDtos.FeePlanRequest("IELTS Foundation – trọn khóa", courses.get("IELTS-F").id(), new BigDecimal("7200000"), 36, "Thanh toán 1 lần hoặc 2 đợt", true)));
            plans.put("IELTS-65", billingService.createFeePlan(new BillingDtos.FeePlanRequest("IELTS 6.5+ – trọn khóa", courses.get("IELTS-65").id(), new BigDecimal("9800000"), 40, null, true)));
            plans.put("TOEIC-650", billingService.createFeePlan(new BillingDtos.FeePlanRequest("TOEIC 650+ – trọn khóa", courses.get("TOEIC-650").id(), new BigDecimal("5400000"), 30, null, true)));
            plans.put("KIDS-ST", billingService.createFeePlan(new BillingDtos.FeePlanRequest("Thiếu nhi – học phí tháng (8 buổi)", courses.get("KIDS-ST").id(), new BigDecimal("1200000"), 8, "Thu đầu mỗi tháng", true)));
            plans.put("GT-PX", billingService.createFeePlan(new BillingDtos.FeePlanRequest("Giao tiếp – trọn khóa", courses.get("GT-PX").id(), new BigDecimal("4200000"), 24, null, true)));
            billingService.createFeePlan(new BillingDtos.FeePlanRequest("Phí giáo trình", null, new BigDecimal("350000"), null, "Bộ sách + tài liệu in", true));
        });

        // Lớp: (mã, tên, khóa, GV, phòng, sĩ số, bắt đầu cách đây n tuần, lịch)
        record ClassSpec(String code, String name, String course, String teacher, String room, int max, int weeksAgo, int weeks,
                         int[] days, String start, String end) {}
        List<ClassSpec> specs = List.of(
                new ClassSpec("IELTS-F-K12", "IELTS Foundation K12", "IELTS-F", "david", "P101", 18, 7, 18, new int[]{1, 3, 5}, "18:00", "19:30"),
                new ClassSpec("IELTS-65-K03", "IELTS 6.5+ K03", "IELTS-65", "david", "P102", 14, 5, 20, new int[]{2, 4}, "19:45", "21:15"),
                new ClassSpec("TOEIC-A2", "TOEIC 650+ – Tối 2-4-6", "TOEIC-650", "quang", "P201", 22, 6, 15, new int[]{1, 3, 5}, "19:45", "21:15"),
                new ClassSpec("KIDS-ST-01", "Starters Cuối tuần 01", "KIDS-ST", "hanh", "P101", 16, 9, 24, new int[]{6, 7}, "08:30", "10:00"),
                new ClassSpec("GT-PX-05", "Giao tiếp phản xạ K05", "GT-PX", "hanh", "P102", 12, 4, 12, new int[]{2, 4}, "18:00", "19:30"));

        Map<String, ClassDtos.ClassDetail> classes = new LinkedHashMap<>();
        for (ClassSpec sp : specs) {
            LocalDate start = today.minusWeeks(sp.weeksAgo()).with(DayOfWeek.MONDAY);
            ClassDtos.ClassDetail c = tx.execute(s -> {
                ClassDtos.ClassDetail created = classService.create(new ClassDtos.ClassRequest(courses.get(sp.course()).id(), sp.code(), sp.name(),
                        users.get(sp.teacher()).getId(), rooms.get(sp.room()), sp.max(), start, start.plusWeeks(sp.weeks()), ClassGroup.Status.ONGOING, null));
                List<ClassDtos.SlotDto> slots = Arrays.stream(sp.days()).mapToObj(d -> new ClassDtos.SlotDto(d, LocalTime.parse(sp.start()), LocalTime.parse(sp.end()))).toList();
                return classService.replaceSchedules(created.id(), new ClassDtos.SchedulesRequest(slots));
            });
            classes.put(sp.code(), c);
        }
        // Lớp sắp mở (chưa có học viên) để demo trạng thái
        tx.executeWithoutResult(s -> {
            LocalDate start = today.plusWeeks(2).with(DayOfWeek.MONDAY);
            ClassDtos.ClassDetail c = classService.create(new ClassDtos.ClassRequest(courses.get("IELTS-F").id(), "IELTS-F-K13", "IELTS Foundation K13",
                    users.get("quang").getId(), rooms.get("P201"), 20, start, start.plusWeeks(18), ClassGroup.Status.PLANNED, "Khai giảng sau 2 tuần"));
            classService.replaceSchedules(c.id(), new ClassDtos.SchedulesRequest(List.of(
                    new ClassDtos.SlotDto(2, LocalTime.of(18, 0), LocalTime.of(19, 30)),
                    new ClassDtos.SlotDto(4, LocalTime.of(18, 0), LocalTime.of(19, 30)))));
        });

        // Học viên
        String[][] people = {
                {"Nguyễn Gia Hân", "2014-03-12", "FEMALE", "Nguyễn Văn Hùng", "Bố", "0908111222", "phuhuynh@hoasen.edu.vn"},
                {"Trần Minh Khang", "2013-07-25", "MALE", "Phạm Thị Thu Trang", "Mẹ", "0909222333", "thutrang.pham@gmail.com"},
                {"Lê Hoàng Bảo Anh", "2015-01-08", "FEMALE", "Lê Hoàng Nam", "Bố", "0913333444", "lehoangnam80@gmail.com"},
                {"Phạm Đức Minh", "2006-11-30", "MALE", null, null, null, null},
                {"Võ Thị Ngọc Trâm", "2005-04-17", "FEMALE", null, null, null, null},
                {"Đặng Quốc Huy", "2004-09-02", "MALE", null, null, null, null},
                {"Bùi Thanh Tâm", "1998-12-21", "FEMALE", null, null, null, null},
                {"Huỳnh Tấn Phát", "1995-06-14", "MALE", null, null, null, null},
                {"Ngô Thảo Vy", "2007-02-27", "FEMALE", "Ngô Văn Lực", "Bố", "0918444555", "ngovanluc@gmail.com"},
                {"Đỗ Minh Quân", "2008-08-09", "MALE", "Trịnh Thị Mai", "Mẹ", "0918555666", "trinhmai.hcm@gmail.com"},
                {"Hồ Ngọc Diệp", "2003-05-19", "FEMALE", null, null, null, null},
                {"Dương Khánh Linh", "2016-10-03", "FEMALE", "Dương Văn Tú", "Bố", "0932666777", "duongtu.kd@gmail.com"},
                {"Lý Gia Bảo", "2016-04-22", "MALE", "Lý Thị Kim", "Mẹ", "0932777888", "lykim1988@gmail.com"},
                {"Mai Anh Thư", "2015-12-11", "FEMALE", "Mai Văn Đông", "Bố", "0932888999", "maidong.it@gmail.com"},
                {"Trương Nhật Nam", "2016-07-07", "MALE", "Trương Thị Hoa", "Mẹ", "0937111000", "truonghoa.gv@gmail.com"},
                {"Phan Bảo Ngọc", "2015-09-29", "FEMALE", "Phan Văn Tài", "Bố", "0937222000", "phantai.xd@gmail.com"},
                {"Vũ Hải Đăng", "2001-01-15", "MALE", null, null, null, null},
                {"Tạ Thu Hà", "1999-03-08", "FEMALE", null, null, null, null},
                {"Lâm Chí Thanh", "1993-10-20", "MALE", null, null, null, null},
                {"Châu Mỹ Duyên", "2000-06-01", "FEMALE", null, null, null, null},
                {"Kiều Văn Sơn", "1997-02-14", "MALE", null, null, null, null},
                {"Đinh Hoàng Yến", "2002-11-11", "FEMALE", null, null, null, null},
                {"Quách Tuấn Kiệt", "2006-05-05", "MALE", "Quách Văn Long", "Bố", "0977333111", "quachlong@gmail.com"},
                {"Tôn Nữ Minh Châu", "2005-08-18", "FEMALE", null, null, null, null},
                {"La Quốc Việt", "2004-12-24", "MALE", null, null, null, null},
                {"Mạc Thị Hồng Nhung", "1996-07-30", "FEMALE", null, null, null, null},
                {"Hà Phương Uyên", "2015-03-03", "FEMALE", "Hà Văn Bình", "Bố", "0977444222", "habinh.bs@gmail.com"},
                {"Cao Minh Trí", "2007-09-09", "MALE", "Cao Thị Lụa", "Mẹ", "0977555333", "caolua1979@gmail.com"},
                {"Nguyễn Thị Kim Ngân", "1994-04-04", "FEMALE", null, null, null, null},
                {"Trần Quốc Bảo", "2003-02-02", "MALE", null, null, null, null},
                {"Lưu Gia Linh", "2016-01-20", "FEMALE", "Lưu Văn Phúc", "Bố", "0966111444", "luuphuc.ck@gmail.com"},
                {"Âu Dương Phong", "2005-10-10", "MALE", null, null, null, null},
        };
        List<Long> studentIds = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            String[] p = people[i];
            int idx = i;
            Long id = tx.execute(s -> {
                String phone = p[3] == null ? "09" + String.format("%08d", 10_000_000 + idx * 7919 % 89_999_999) : null;
                String email = p[3] == null ? vn.educenter.common.VnText.stripAccents(p[0]).toLowerCase().replace(" ", ".") + "@gmail.com" : null;
                StudentDtos.StudentResponse st = studentService.create(new StudentDtos.StudentRequest(p[0], LocalDate.parse(p[1]),
                        Student.Gender.valueOf(p[2]), phone, email, p[3] != null ? "THCS/Tiểu học Quận 1" : null,
                        null, null, Student.Status.ACTIVE, today.minusDays(300 - idx * 9L), null));
                if (p[3] != null) {
                    studentService.addGuardian(st.id(), new StudentDtos.GuardianRequest(p[3], p[4], p[5], p[6], p[5], true));
                }
                return st.id();
            });
            studentIds.add(id);
        }

        // Ghi danh
        int[][] enrollPlan = {
                {0, 1, 2, 11, 12, 13, 14, 15, 26, 30},                  // KIDS (index tham chiếu dưới)
                {3, 4, 5, 8, 9, 22, 23, 24, 27, 31},                     // IELTS-F
                {6, 10, 16, 17, 19, 21, 29},                              // IELTS-65
                {7, 18, 20, 25, 28, 29, 16, 10, 21},                      // TOEIC
                {6, 7, 18, 20, 25, 28},                                    // GT-PX
        };
        String[] enrollClass = {"KIDS-ST-01", "IELTS-F-K12", "IELTS-65-K03", "TOEIC-A2", "GT-PX-05"};
        for (int k = 0; k < enrollClass.length; k++) {
            ClassDtos.ClassDetail c = classes.get(enrollClass[k]);
            for (int si : enrollPlan[k]) {
                Long sid = studentIds.get(si);
                tx.executeWithoutResult(s -> classService.enroll(c.id(), new ClassDtos.EnrollRequest(sid, c.startDate())));
            }
        }
        // Trạng thái đa dạng
        tx.executeWithoutResult(s -> {
            studentService.updateStatus(studentIds.get(31), Student.Status.ON_HOLD);
            studentService.updateStatus(studentIds.get(26), Student.Status.DROPPED);
            Student trial = studentRepository.findById(studentService.create(new StudentDtos.StudentRequest("Nguyễn Hoàng Long", LocalDate.of(2009, 6, 6),
                    Student.Gender.MALE, "0981234567", "hoanglong.ng@gmail.com", "THPT Lê Quý Đôn", "A2", null, Student.Status.TRIAL, today.minusDays(3), "Học thử lớp IELTS Foundation")).id()).orElseThrow();
            trial.setNote("Học thử 2 buổi, hẹn tư vấn lộ trình");
        });

        // Buổi học trong quá khứ + điểm danh
        Long staffId = users.get("staff").getId();
        for (ClassDtos.ClassDetail c : classes.values()) {
            tx.executeWithoutResult(s -> seedPastSessions(t.getId(), c, today, staffId));
        }

        // Tài khoản cổng phụ huynh cho bé Gia Hân
        tx.executeWithoutResult(s -> studentService.upsertPortalAccount(studentIds.get(0),
                new StudentDtos.PortalAccountRequest("phuhuynh@hoasen.edu.vn", DEMO_PASSWORD)));

        // Hóa đơn & thanh toán
        tx.executeWithoutResult(s -> seedInvoices(t.getId(), today, classes, plans, enrollClass));
    }

    /** Buổi học quá khứ đã được sinh khi lưu lịch (từ ngày khai giảng): điểm danh ngẫu nhiên, thỉnh thoảng hủy. */
    private void seedPastSessions(Long tenantId, ClassDtos.ClassDetail c, LocalDate today, Long staffId) {
        List<Enrollment> enrolled = enrollmentRepository.findByTenantIdAndClassIdOrderByStatusAscIdAsc(tenantId, c.id());
        for (ClassSession s : sessionRepository.findByTenantIdAndClassIdOrderBySessionDateAscStartTimeAsc(tenantId, c.id())) {
            if (!s.getSessionDate().isBefore(today)) break;
            boolean cancelled = random.nextInt(40) == 0;
            s.setStatus(cancelled ? ClassSession.Status.CANCELLED : ClassSession.Status.COMPLETED);
            if (cancelled) {
                s.setNote("Nghỉ lễ / giáo viên ốm – sẽ học bù");
                continue;
            }
            Instant markedAt = s.getSessionDate().atTime(s.getEndTime()).atZone(TimeConfig.VN).toInstant();
            for (Enrollment e : enrolled) {
                if (e.getStartDate().isAfter(s.getSessionDate())) continue;
                Attendance a = new Attendance();
                a.setTenantId(tenantId);
                a.setSessionId(s.getId());
                a.setStudentId(e.getStudentId());
                int r = random.nextInt(100);
                a.setStatus(r < 84 ? Attendance.Status.PRESENT : r < 91 ? Attendance.Status.LATE : r < 96 ? Attendance.Status.EXCUSED : Attendance.Status.ABSENT);
                if (a.getStatus() == Attendance.Status.EXCUSED) a.setNote("Phụ huynh xin phép");
                a.setMarkedBy(c.teacherId() != null ? c.teacherId() : staffId);
                a.setMarkedAt(markedAt);
                attendanceRepository.save(a);
            }
        }
    }

    private void seedInvoices(Long tenantId, LocalDate today, Map<String, ClassDtos.ClassDetail> classes,
                              Map<String, BillingDtos.FeePlanResponse> plans, String[] enrollClass) {
        String[] planOf = {"KIDS-ST", "IELTS-F", "IELTS-65", "TOEIC-650", "GT-PX"};
        int n = 0;
        for (int k = 0; k < enrollClass.length; k++) {
            ClassDtos.ClassDetail c = classes.get(enrollClass[k]);
            BillingDtos.FeePlanResponse plan = plans.get(planOf[k]);
            for (Enrollment e : enrollmentRepository.findByTenantIdAndClassIdOrderByStatusAscIdAsc(tenantId, c.id())) {
                n++;
                if (planOf[k].equals("KIDS-ST")) {
                    // Học phí tháng: 3 tháng gần nhất
                    for (int m = 2; m >= 0; m--) {
                        YearMonth ym = YearMonth.from(today).minusMonths(m);
                        LocalDate due = ym.atDay(10);
                        var inv = billingService.createInvoice(new BillingDtos.InvoiceRequest(e.getStudentId(), c.id(), plan.id(),
                                "Học phí Starters tháng " + ym.getMonthValue() + "/" + ym.getYear(), null, null, due, null));
                        boolean pay = m > 0 ? random.nextInt(10) < 9 : random.nextInt(10) < 5;
                        if (pay) pay(inv.id(), inv.total(), ym.atDay(3 + random.nextInt(8)), random.nextBoolean() ? Payment.Method.BANK_TRANSFER : Payment.Method.CASH, today);
                    }
                } else {
                    BigDecimal discount = n % 5 == 0 ? new BigDecimal("500000") : BigDecimal.ZERO;
                    LocalDate due = c.startDate().plusDays(14);
                    if (n % 7 == 3) due = today.plusDays(2); // sắp đến hạn → nhắc học phí
                    var inv = billingService.createInvoice(new BillingDtos.InvoiceRequest(e.getStudentId(), c.id(), plan.id(), null, null,
                            discount, due, discount.signum() > 0 ? "Giảm 500.000 ₫ – học viên cũ giới thiệu" : null));
                    int mode = n % 4;
                    LocalDate paidDay = c.startDate().minusDays(random.nextInt(5));
                    if (mode == 0 || mode == 1) {
                        pay(inv.id(), inv.total(), paidDay, Payment.Method.BANK_TRANSFER, today);
                    } else if (mode == 2) {
                        BigDecimal half = inv.total().divide(BigDecimal.valueOf(2)).setScale(-3, java.math.RoundingMode.DOWN);
                        pay(inv.id(), half, paidDay, Payment.Method.CASH, today);
                        if (n % 3 == 0) pay(inv.id(), inv.total().subtract(half), paidDay.plusDays(30), Payment.Method.BANK_TRANSFER, today);
                    }
                    // mode 3: chưa đóng (một số quá hạn)
                }
            }
        }
        // Doanh thu các tháng trước (khóa đã kết thúc) để biểu đồ 12 tháng có dữ liệu
        List<Long> ids = studentRepository.findAll().stream().filter(s -> s.getTenantId().equals(tenantId)).map(Student::getId).toList();
        for (int m = 11; m >= 3; m--) {
            YearMonth ym = YearMonth.from(today).minusMonths(m);
            int count = 6 + random.nextInt(7);
            for (int i = 0; i < count; i++) {
                Long sid = ids.get(random.nextInt(ids.size()));
                BigDecimal amount = new BigDecimal(new int[]{4200000, 5400000, 7200000, 1200000, 9800000}[random.nextInt(5)]);
                var inv = billingService.createInvoice(new BillingDtos.InvoiceRequest(sid, null, null,
                        "Học phí khóa trước (" + ym.getMonthValue() + "/" + ym.getYear() + ")", amount, null, ym.atDay(15), null));
                pay(inv.id(), amount, ym.atDay(1 + random.nextInt(27)), Payment.Method.BANK_TRANSFER, today);
            }
        }
    }

    private void pay(Long invoiceId, BigDecimal amount, LocalDate day, Payment.Method method, LocalDate today) {
        LocalDate d = day.isAfter(today) ? today : day;
        Instant at = d.atTime(9 + random.nextInt(10), random.nextInt(60)).atZone(TimeConfig.VN).toInstant();
        if (at.isAfter(clock.instant())) at = clock.instant().minusSeconds(3600);
        billingService.recordPayment(invoiceId, new BillingDtos.PaymentRequest(amount, method, at, null));
    }

    // ------------------------------------------------------------------ Sao Mai (tenant thứ 2 – kiểm tra cô lập)

    private void seedSaoMai(Tenant t) {
        LocalDate today = LocalDate.now(clock);
        User admin = tx.execute(s -> {
            User a = user(t.getId(), "chu@saomai.edu.vn", "Hoàng Văn Mai", "0905666777", Role.CENTER_ADMIN);
            user(t.getId(), "gv.trang@saomai.edu.vn", "Nguyễn Thu Trang", "0905666888", Role.TEACHER);
            return a;
        });
        actAs(admin);
        tx.executeWithoutResult(s -> {
            Long room = catalogService.createRoom(new CatalogDtos.RoomRequest("Phòng A", 15, true)).id();
            var course = catalogService.createCourse(new CatalogDtos.CourseRequest("KET", "Cambridge KET", "A2", 32, new BigDecimal("6000000"), null, true));
            Long teacher = userRepository.findByEmailIgnoreCase("gv.trang@saomai.edu.vn").orElseThrow().getId();
            LocalDate start = today.minusWeeks(2).with(DayOfWeek.MONDAY);
            var c = classService.create(new ClassDtos.ClassRequest(course.id(), "KET-01", "KET Sáng thứ 7", teacher, room, 15, start, start.plusWeeks(16), ClassGroup.Status.ONGOING, null));
            classService.replaceSchedules(c.id(), new ClassDtos.SchedulesRequest(List.of(new ClassDtos.SlotDto(6, LocalTime.of(9, 0), LocalTime.of(10, 30)))));
            for (String name : List.of("Phan Thị Mỹ Linh", "Trịnh Công Sơn", "Lê Thị Bích Ngọc")) {
                var st = studentService.create(new StudentDtos.StudentRequest(name, null, null, null, null, null, null, null, Student.Status.ACTIVE, today.minusDays(20), null));
                classService.enroll(c.id(), new ClassDtos.EnrollRequest(st.id(), start));
            }
        });
    }
}
