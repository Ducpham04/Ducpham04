package vn.educenter.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.billing.Invoice;
import vn.educenter.billing.InvoiceRepository;
import vn.educenter.billing.VietQrService;
import vn.educenter.catalog.Room;
import vn.educenter.catalog.RoomRepository;
import vn.educenter.classes.*;
import vn.educenter.common.VnText;
import vn.educenter.config.AppProperties;
import vn.educenter.config.TimeConfig;
import vn.educenter.student.Guardian;
import vn.educenter.student.GuardianRepository;
import vn.educenter.student.Student;
import vn.educenter.student.StudentRepository;
import vn.educenter.tenant.Tenant;
import vn.educenter.tenant.TenantRepository;

/**
 * Nhắc lịch học (buổi ngày mai) và nhắc học phí (sắp/quá hạn) qua Email + Zalo ZNS.
 * Chạy theo từng tenant với tenantId tường minh (không phụ thuộc người dùng đăng nhập).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<NotificationLog.Status> DONE = EnumSet.of(NotificationLog.Status.SENT, NotificationLog.Status.MOCK);

    private final TenantRepository tenantRepository;
    private final ClassSessionRepository sessionRepository;
    private final ClassGroupRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationLogRepository logRepository;
    private final EmailSender emailSender;
    private final ZaloZnsClient zaloZnsClient;
    private final AppProperties props;
    private final Clock clock;

    public record RunResult(int sent, int failed, int skipped) {}

    private record Contact(String email, String phone, String name) {}

    @Transactional
    public RunResult runSessionReminders(Long onlyTenantId) {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        int[] counter = new int[3];
        for (Tenant tenant : tenants(onlyTenantId)) {
            List<ClassSession> sessions = sessionRepository.findRange(tenant.getId(), tomorrow, tomorrow, null, null, null).stream()
                    .filter(s -> s.getStatus() == ClassSession.Status.SCHEDULED).toList();
            if (sessions.isEmpty()) continue;
            Set<Long> classIds = sessions.stream().map(ClassSession::getClassId).collect(Collectors.toSet());
            Map<Long, ClassGroup> classes = classRepository.findByTenantIdAndIdIn(tenant.getId(), classIds).stream()
                    .collect(Collectors.toMap(ClassGroup::getId, Function.identity()));
            Map<Long, List<Long>> studentsByClass = enrollmentRepository.findActiveByClassIds(tenant.getId(), classIds).stream()
                    .collect(Collectors.groupingBy(Enrollment::getClassId, Collectors.mapping(Enrollment::getStudentId, Collectors.toList())));
            Set<Long> roomIds = sessions.stream().map(ClassSession::getRoomId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> rooms = roomIds.isEmpty() ? Map.of() : roomRepository.findByTenantIdAndIdIn(tenant.getId(), roomIds).stream()
                    .collect(Collectors.toMap(Room::getId, Room::getName));
            Map<Long, Contact> contacts = contacts(tenant.getId(), studentsByClass.values().stream().flatMap(List::stream).collect(Collectors.toSet()));
            for (ClassSession s : sessions) {
                ClassGroup c = classes.get(s.getClassId());
                for (Long studentId : studentsByClass.getOrDefault(s.getClassId(), List.of())) {
                    Contact ct = contacts.get(studentId);
                    if (ct == null) continue;
                    String ref = "SESSION:" + s.getId() + ":" + studentId;
                    String subject = "[" + tenant.getName() + "] Nhắc lịch học ngày " + D.format(s.getSessionDate());
                    String body = """
                            Kính gửi Quý phụ huynh/học viên,

                            %s có buổi học lớp %s (%s) vào ngày mai:
                              • Thời gian: %s – %s, %s
                              • Phòng: %s

                            Vui lòng đến đúng giờ. Nếu vắng, xin báo trước cho trung tâm qua số %s.

                            Trân trọng,
                            %s
                            """.formatted(ct.name(), c.getName(), c.getCode(), s.getStartTime(), s.getEndTime(),
                            D.format(s.getSessionDate()), rooms.getOrDefault(s.getRoomId(), "sẽ thông báo"),
                            Objects.requireNonNullElse(tenant.getPhone(), "của trung tâm"), tenant.getName());
                    deliver(tenant.getId(), NotificationLog.Type.SESSION_REMINDER, ref, ct, subject, body,
                            "SESSION_REMINDER_TEMPLATE", Map.of("student_name", ct.name(), "class_name", c.getName(),
                                    "date", D.format(s.getSessionDate()), "time", s.getStartTime().toString()), counter);
                }
            }
        }
        return new RunResult(counter[0], counter[1], counter[2]);
    }

    @Transactional
    public RunResult runFeeReminders(Long onlyTenantId) {
        LocalDate today = LocalDate.now(clock);
        Instant startOfToday = today.atStartOfDay(TimeConfig.VN).toInstant();
        int[] counter = new int[3];
        for (Tenant tenant : tenants(onlyTenantId)) {
            List<Invoice> invoices = invoiceRepository.findToRemind(tenant.getId(), today.plusDays(props.reminders().feeDaysBefore()), startOfToday);
            if (invoices.isEmpty()) continue;
            Map<Long, Contact> contacts = contacts(tenant.getId(), invoices.stream().map(Invoice::getStudentId).collect(Collectors.toSet()));
            for (Invoice inv : invoices) {
                Contact ct = contacts.get(inv.getStudentId());
                if (ct == null) continue;
                boolean overdue = inv.isOverdue(today);
                String ref = "FEE:" + inv.getId() + ":" + today;
                String subject = "[" + tenant.getName() + "] " + (overdue ? "Học phí đã quá hạn" : "Nhắc hạn đóng học phí") + " – " + inv.getCode();
                String bank = tenant.hasBankInfo()
                        ? "\nChuyển khoản: %s – STK %s – %s\nNội dung: %s\nMã QR: %s\n".formatted(
                        VietQrService.BANKS.getOrDefault(tenant.getBankCode(), tenant.getBankCode()), tenant.getBankAccountNo(),
                        Objects.requireNonNullElse(tenant.getBankAccountName(), ""), VnText.transferNote(inv.getCode(), ct.name()),
                        VietQrService.buildImageUrl(tenant.getBankCode(), tenant.getBankAccountNo(), tenant.getBankAccountName(),
                                inv.remaining(), VnText.transferNote(inv.getCode(), ct.name())))
                        : "";
                String body = """
                        Kính gửi Quý phụ huynh/học viên,

                        Hóa đơn %s của %s – "%s"
                          • Số tiền còn lại: %s
                          • Hạn thanh toán: %s%s
                        %s
                        Nếu đã thanh toán, xin vui lòng bỏ qua email này.

                        Trân trọng,
                        %s
                        """.formatted(inv.getCode(), ct.name(), inv.getDescription(), VnText.formatVnd(inv.remaining()),
                        D.format(inv.getDueDate()), overdue ? " (đã quá hạn)" : "", bank, tenant.getName());
                boolean ok = deliver(tenant.getId(), NotificationLog.Type.FEE_REMINDER, ref, ct, subject, body, "FEE_REMINDER_TEMPLATE",
                        Map.of("student_name", ct.name(), "invoice_code", inv.getCode(),
                                "amount", inv.remaining().toPlainString(), "due_date", D.format(inv.getDueDate())), counter);
                if (ok) inv.setLastRemindedAt(clock.instant());
            }
        }
        return new RunResult(counter[0], counter[1], counter[2]);
    }

    /** Gửi email (nếu có) + Zalo ZNS (nếu có SĐT). Trả true nếu ít nhất 1 kênh thành công. counter = [sent, failed, skipped]. */
    private boolean deliver(Long tenantId, NotificationLog.Type type, String ref, Contact ct, String subject, String body,
                            String templateId, Map<String, String> znsData, int[] counter) {
        boolean anyOk = false;
        if (ct.email() != null) {
            if (logRepository.existsByRefKeyAndChannelAndStatusIn(ref, NotificationLog.Channel.EMAIL, DONE)) {
                counter[2]++;
            } else {
                try {
                    emailSender.send(ct.email(), subject, body);
                    log(tenantId, NotificationLog.Channel.EMAIL, type, ct.email(), subject, NotificationLog.Status.SENT, null, ref);
                    counter[0]++;
                    anyOk = true;
                } catch (RuntimeException e) {
                    log.warn("Gửi email nhắc thất bại tới {}: {}", ct.email(), e.getMessage());
                    log(tenantId, NotificationLog.Channel.EMAIL, type, ct.email(), subject, NotificationLog.Status.FAILED, e.getMessage(), ref);
                    counter[1]++;
                }
            }
        }
        if (ct.phone() != null && !logRepository.existsByRefKeyAndChannelAndStatusIn(ref, NotificationLog.Channel.ZALO_ZNS, DONE)) {
            ZaloZnsClient.Result r = zaloZnsClient.sendTemplate(ct.phone(), templateId, znsData, ref);
            NotificationLog.Status st = !r.success() ? NotificationLog.Status.FAILED : r.mock() ? NotificationLog.Status.MOCK : NotificationLog.Status.SENT;
            log(tenantId, NotificationLog.Channel.ZALO_ZNS, type, ct.phone(), subject, st, r.error(), ref);
            anyOk |= r.success();
        }
        return anyOk;
    }

    private void log(Long tenantId, NotificationLog.Channel channel, NotificationLog.Type type, String recipient, String subject,
                     NotificationLog.Status status, String error, String ref) {
        NotificationLog l = new NotificationLog();
        l.setTenantId(tenantId);
        l.setChannel(channel);
        l.setType(type);
        l.setRecipient(recipient);
        l.setSubject(subject.length() > 255 ? subject.substring(0, 255) : subject);
        l.setStatus(status);
        l.setError(error == null ? null : error.substring(0, Math.min(500, error.length())));
        l.setRefKey(ref);
        l.setCreatedAt(clock.instant());
        logRepository.save(l);
    }

    /** Liên hệ ưu tiên: phụ huynh chính (email/Zalo/SĐT), fallback thông tin học viên. */
    private Map<Long, Contact> contacts(Long tenantId, Set<Long> studentIds) {
        if (studentIds.isEmpty()) return Map.of();
        Map<Long, Student> students = studentRepository.findByTenantIdAndIdIn(tenantId, studentIds).stream()
                .filter(s -> s.getStatus() != Student.Status.DROPPED)
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        Map<Long, Guardian> primary = new HashMap<>();
        guardianRepository.findByTenantIdAndStudentIdIn(tenantId, studentIds).stream()
                .sorted(Comparator.comparing(Guardian::isPrimary).reversed())
                .forEach(g -> primary.putIfAbsent(g.getStudentId(), g));
        Map<Long, Contact> result = new HashMap<>();
        students.forEach((id, s) -> {
            Guardian g = primary.get(id);
            String email = firstNonBlank(g == null ? null : g.getEmail(), s.getEmail());
            String phone = firstNonBlank(g == null ? null : g.getZaloPhone(), g == null ? null : g.getPhone(), s.getPhone());
            if (email != null || phone != null) result.put(id, new Contact(email, phone, s.getFullName()));
        });
        return result;
    }

    private List<Tenant> tenants(Long onlyTenantId) {
        if (onlyTenantId != null) return tenantRepository.findById(onlyTenantId).stream().toList();
        return tenantRepository.findByStatus(Tenant.Status.ACTIVE);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }
}
