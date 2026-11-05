package vn.educenter.portal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.attendance.AttendanceDtos;
import vn.educenter.attendance.AttendanceService;
import vn.educenter.billing.BillingDtos;
import vn.educenter.billing.BillingService;
import vn.educenter.billing.Invoice;
import vn.educenter.billing.InvoiceRepository;
import vn.educenter.classes.*;
import vn.educenter.common.ForbiddenException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.AuthUser;
import vn.educenter.security.CurrentUser;
import vn.educenter.student.Student;
import vn.educenter.student.StudentRepository;
import vn.educenter.tenant.Tenant;
import vn.educenter.tenant.TenantRepository;

/** Cổng phụ huynh/học viên: chỉ đọc dữ liệu của đúng học viên gắn với tài khoản. */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final StudentRepository studentRepository;
    private final TenantRepository tenantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository sessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final BillingService billingService;
    private final ClassService classService;
    private final Clock clock;

    public record PortalMe(Long studentId, String studentCode, String fullName, Student.Status status, String centerName,
                           String centerPhone, String centerAddress, List<ClassDtos.ClassListItem> classes,
                           ClassDtos.SessionView nextSession, double attendanceRate, java.math.BigDecimal outstanding) {}

    @Transactional(readOnly = true)
    public PortalMe me() {
        Student s = currentStudent();
        Tenant t = tenantRepository.findById(s.getTenantId()).orElseThrow();
        List<ClassDtos.SessionView> upcoming = sessions();
        AttendanceDtos.StudentAttendance att = attendanceService.historyOf(s.getTenantId(), s.getId());
        java.math.BigDecimal outstanding = invoiceRepository.findByTenantIdAndStudentIdOrderByCreatedAtDesc(s.getTenantId(), s.getId())
                .stream().filter(i -> i.getStatus() == Invoice.Status.UNPAID || i.getStatus() == Invoice.Status.PARTIAL)
                .map(Invoice::remaining).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return new PortalMe(s.getId(), s.getCode(), s.getFullName(), s.getStatus(), t.getName(), t.getPhone(), t.getAddress(),
                activeClasses(s), upcoming.isEmpty() ? null : upcoming.getFirst(), att.rate(), outstanding);
    }

    @Transactional(readOnly = true)
    public List<ClassDtos.SessionView> sessions() {
        Student s = currentStudent();
        List<Long> classIds = enrollmentRepository.findByTenantIdAndStudentIdAndStatus(s.getTenantId(), s.getId(), Enrollment.Status.ACTIVE)
                .stream().map(Enrollment::getClassId).toList();
        if (classIds.isEmpty()) return List.of();
        LocalDate today = LocalDate.now(clock);
        return sessionService.toViewsPublic(s.getTenantId(), sessionRepository.findForClasses(s.getTenantId(), classIds, today, today.plusDays(30)));
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.StudentAttendance attendance() {
        Student s = currentStudent();
        return attendanceService.historyOf(s.getTenantId(), s.getId());
    }

    @Transactional(readOnly = true)
    public List<BillingDtos.InvoiceResponse> invoices() {
        Student s = currentStudent();
        return billingService.invoicesOfStudent(s.getTenantId(), s.getId()).stream()
                .filter(i -> i.status() != Invoice.Status.CANCELLED).toList();
    }

    @Transactional(readOnly = true)
    public BillingDtos.VietQrResponse vietQr(Long invoiceId) {
        Student s = currentStudent();
        Invoice inv = invoiceRepository.findByIdAndTenantId(invoiceId, s.getTenantId())
                .filter(i -> i.getStudentId().equals(s.getId()))
                .orElseThrow(() -> NotFoundException.of("hóa đơn"));
        return billingService.qrFor(tenantRepository.findById(s.getTenantId()).orElseThrow(), inv);
    }

    private List<ClassDtos.ClassListItem> activeClasses(Student s) {
        List<Long> classIds = enrollmentRepository.findByTenantIdAndStudentIdAndStatus(s.getTenantId(), s.getId(), Enrollment.Status.ACTIVE)
                .stream().map(Enrollment::getClassId).toList();
        return classService.listForPortal(s.getTenantId(), classIds);
    }

    private Student currentStudent() {
        AuthUser u = CurrentUser.get();
        if (u.studentId() == null || u.tenantId() == null) throw new ForbiddenException();
        return studentRepository.findByIdAndTenantId(u.studentId(), u.tenantId()).orElseThrow(ForbiddenException::new);
    }
}
