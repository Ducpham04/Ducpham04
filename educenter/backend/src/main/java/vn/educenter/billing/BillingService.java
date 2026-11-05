package vn.educenter.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.catalog.CatalogService;
import vn.educenter.classes.ClassGroup;
import vn.educenter.classes.ClassService;
import vn.educenter.classes.Lookups;
import vn.educenter.common.*;
import vn.educenter.security.CurrentUser;
import vn.educenter.student.Guardian;
import vn.educenter.student.GuardianRepository;
import vn.educenter.student.Student;
import vn.educenter.student.StudentService;
import vn.educenter.tenant.Tenant;
import vn.educenter.tenant.TenantService;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");

    private final FeePlanRepository feePlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentService studentService;
    private final ClassService classService;
    private final CatalogService catalogService;
    private final TenantService tenantService;
    private final CodeSequenceService codeSequenceService;
    private final VietQrService vietQrService;
    private final ReceiptPdfService receiptPdfService;
    private final Lookups lookups;
    private final Clock clock;

    // ---------- Gói học phí ----------

    @Transactional(readOnly = true)
    public List<BillingDtos.FeePlanResponse> feePlans() {
        Long tenantId = CurrentUser.tenantId();
        List<FeePlan> plans = feePlanRepository.findByTenantIdOrderByNameAsc(tenantId);
        Map<Long, String> courses = lookups.courseNames(tenantId, plans.stream().map(FeePlan::getCourseId).toList());
        return plans.stream().map(p -> toPlan(p, courses.get(p.getCourseId()))).toList();
    }

    @Transactional
    public BillingDtos.FeePlanResponse createFeePlan(BillingDtos.FeePlanRequest req) {
        FeePlan p = new FeePlan();
        p.setTenantId(CurrentUser.tenantId());
        applyPlan(p, req);
        feePlanRepository.save(p);
        return toPlan(p, courseName(p.getCourseId()));
    }

    @Transactional
    public BillingDtos.FeePlanResponse updateFeePlan(Long id, BillingDtos.FeePlanRequest req) {
        FeePlan p = findPlan(id);
        applyPlan(p, req);
        return toPlan(p, courseName(p.getCourseId()));
    }

    @Transactional
    public void deleteFeePlan(Long id) {
        feePlanRepository.delete(findPlan(id));
    }

    // ---------- Hóa đơn ----------

    @Transactional(readOnly = true)
    public PageResponse<BillingDtos.InvoiceResponse> searchInvoices(String status, Long studentId, String q, int page, int size) {
        Long tenantId = CurrentUser.tenantId();
        LocalDate today = LocalDate.now(clock);
        Collection<Invoice.Status> statuses = EnumSet.allOf(Invoice.Status.class);
        LocalDate overdueBefore = null;
        if (status != null && !status.isBlank()) {
            if ("OVERDUE".equals(status)) {
                statuses = EnumSet.of(Invoice.Status.UNPAID, Invoice.Status.PARTIAL);
                overdueBefore = today;
            } else if ("OPEN".equals(status)) {
                statuses = EnumSet.of(Invoice.Status.UNPAID, Invoice.Status.PARTIAL);
            } else {
                try {
                    statuses = EnumSet.of(Invoice.Status.valueOf(status));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Trạng thái không hợp lệ");
                }
            }
        }
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<Invoice> result = invoiceRepository.search(tenantId, studentId, statuses, overdueBefore, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, Student> students = lookups.students(tenantId, result.getContent().stream().map(Invoice::getStudentId).toList());
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, result.getContent().stream().map(Invoice::getClassId).toList());
        return PageResponse.of(result, i -> toResponse(i, students.get(i.getStudentId()), classes.get(i.getClassId()), null));
    }

    @Transactional(readOnly = true)
    public List<BillingDtos.InvoiceResponse> invoicesOfStudent(Long tenantId, Long studentId) {
        List<Invoice> list = invoiceRepository.findByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId);
        Map<Long, Student> students = lookups.students(tenantId, List.of(studentId));
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, list.stream().map(Invoice::getClassId).toList());
        return list.stream().map(i -> toResponse(i, students.get(studentId), classes.get(i.getClassId()), null)).toList();
    }

    @Transactional(readOnly = true)
    public BillingDtos.InvoiceResponse getInvoice(Long id) {
        return detail(findInvoice(id));
    }

    @Transactional
    public BillingDtos.InvoiceResponse createInvoice(BillingDtos.InvoiceRequest req) {
        Long tenantId = CurrentUser.tenantId();
        Student s = studentService.find(req.studentId());
        Invoice inv = new Invoice();
        inv.setTenantId(tenantId);
        inv.setStudentId(s.getId());
        String description = req.description();
        BigDecimal amount = req.amount();
        if (req.feePlanId() != null) {
            FeePlan plan = findPlan(req.feePlanId());
            inv.setFeePlanId(plan.getId());
            if (amount == null) amount = plan.getAmount();
            if (description == null || description.isBlank()) description = plan.getName();
        }
        if (req.classId() != null) {
            ClassGroup c = classService.find(req.classId());
            inv.setClassId(c.getId());
            if (description == null || description.isBlank()) description = "Học phí lớp " + c.getCode();
        }
        if (amount == null) throw new BusinessException("Vui lòng chọn gói học phí hoặc nhập số tiền");
        if (description == null || description.isBlank()) throw new BusinessException("Vui lòng nhập nội dung hóa đơn");
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        if (discount.compareTo(amount) > 0) throw new BusinessException("Giảm giá không được lớn hơn số tiền");
        inv.setDescription(description.trim());
        inv.setSubtotal(amount);
        inv.setDiscount(discount);
        inv.setTotal(InvoiceMath.total(amount, discount));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setDueDate(req.dueDate());
        inv.setNote(req.note());
        inv.setStatus(inv.getTotal().signum() == 0 ? Invoice.Status.PAID : Invoice.Status.UNPAID);
        inv.setCreatedBy(CurrentUser.get().userId());
        String yymm = LocalDate.now(clock).format(YYMM);
        inv.setCode(codeSequenceService.nextCode(tenantId, "INVOICE-" + yymm, "HD" + yymm + "-", 4));
        invoiceRepository.save(inv);
        return detail(inv);
    }

    @Transactional
    public BillingDtos.InvoiceResponse cancelInvoice(Long id, String reason) {
        Invoice inv = invoiceRepository.lockByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("hóa đơn"));
        if (inv.getStatus() == Invoice.Status.CANCELLED) throw new BusinessException("Hóa đơn đã hủy trước đó");
        if (inv.getPaidAmount().signum() > 0) throw new BusinessException("Hóa đơn đã có thanh toán, không thể hủy");
        inv.setStatus(Invoice.Status.CANCELLED);
        if (reason != null && !reason.isBlank()) inv.setNote(("Hủy: " + reason.trim()).substring(0, Math.min(500, reason.trim().length() + 5)));
        return detail(inv);
    }

    // ---------- Thanh toán ----------

    @Transactional
    public BillingDtos.InvoiceResponse recordPayment(Long invoiceId, BillingDtos.PaymentRequest req) {
        Long tenantId = CurrentUser.tenantId();
        Invoice inv = invoiceRepository.lockByIdAndTenantId(invoiceId, tenantId).orElseThrow(() -> NotFoundException.of("hóa đơn"));
        if (inv.getStatus() == Invoice.Status.CANCELLED) throw new BusinessException("Hóa đơn đã hủy, không thể thu tiền");
        if (inv.getStatus() == Invoice.Status.PAID) throw new BusinessException("Hóa đơn đã thu đủ");
        BigDecimal remaining = inv.remaining();
        if (req.amount().compareTo(remaining) > 0) {
            throw new BusinessException("Số tiền thu vượt số còn lại (" + VnText.formatVnd(remaining) + ")");
        }
        var now = clock.instant();
        if (req.paidAt() != null && req.paidAt().isAfter(now.plusSeconds(300))) {
            throw new BusinessException("Ngày thu không được ở tương lai");
        }
        Payment p = new Payment();
        p.setTenantId(tenantId);
        p.setInvoiceId(inv.getId());
        p.setAmount(req.amount());
        p.setMethod(req.method());
        p.setPaidAt(req.paidAt() == null ? now : req.paidAt());
        p.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        p.setReceivedBy(CurrentUser.get().userId());
        p.setCreatedAt(now);
        String yymm = LocalDate.now(clock).format(YYMM);
        p.setReceiptNo(codeSequenceService.nextCode(tenantId, "RECEIPT-" + yymm, "PT" + yymm + "-", 4));
        paymentRepository.save(p);
        inv.setPaidAmount(inv.getPaidAmount().add(req.amount()));
        inv.setStatus(InvoiceMath.statusAfterPayment(inv.getTotal(), inv.getPaidAmount()));
        return detail(inv);
    }

    @Transactional(readOnly = true)
    public byte[] receiptPdf(Long paymentId) {
        Long tenantId = CurrentUser.tenantId();
        Payment p = paymentRepository.findByIdAndTenantId(paymentId, tenantId).orElseThrow(() -> NotFoundException.of("phiếu thu"));
        Invoice inv = invoiceRepository.findByIdAndTenantId(p.getInvoiceId(), tenantId).orElseThrow();
        Student s = lookups.students(tenantId, List.of(inv.getStudentId())).get(inv.getStudentId());
        String receiver = lookups.userNames(tenantId, Collections.singletonList(p.getReceivedBy())).get(p.getReceivedBy());
        return receiptPdfService.render(new ReceiptPdfService.ReceiptData(tenantService.current(), inv, p, s.getCode(), s.getFullName(), receiver));
    }

    public String receiptFileName(Long paymentId) {
        return paymentRepository.findByIdAndTenantId(paymentId, CurrentUser.tenantId())
                .map(p -> "phieu-thu-" + p.getReceiptNo() + ".pdf").orElse("phieu-thu.pdf");
    }

    // ---------- Công nợ & QR ----------

    @Transactional(readOnly = true)
    public BillingDtos.DebtSummary debts() {
        Long tenantId = CurrentUser.tenantId();
        LocalDate today = LocalDate.now(clock);
        List<Invoice> open = invoiceRepository.findOpen(tenantId);
        Map<Long, List<Invoice>> byStudent = open.stream().collect(Collectors.groupingBy(Invoice::getStudentId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Student> students = lookups.students(tenantId, byStudent.keySet());
        Map<Long, String> guardianPhones = new HashMap<>();
        if (!byStudent.isEmpty()) {
            guardianRepository.findByTenantIdAndStudentIdIn(tenantId, byStudent.keySet()).stream()
                    .sorted(Comparator.comparing(Guardian::isPrimary).reversed())
                    .forEach(g -> guardianPhones.putIfAbsent(g.getStudentId(), g.getPhone()));
        }
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        List<BillingDtos.DebtItem> items = new ArrayList<>();
        for (var e : byStudent.entrySet()) {
            BigDecimal outstanding = BigDecimal.ZERO;
            BigDecimal overdue = BigDecimal.ZERO;
            LocalDate oldest = null;
            for (Invoice i : e.getValue()) {
                outstanding = outstanding.add(i.remaining());
                if (i.isOverdue(today)) overdue = overdue.add(i.remaining());
                if (oldest == null || i.getDueDate().isBefore(oldest)) oldest = i.getDueDate();
            }
            Student s = students.get(e.getKey());
            totalOutstanding = totalOutstanding.add(outstanding);
            totalOverdue = totalOverdue.add(overdue);
            items.add(new BillingDtos.DebtItem(s.getId(), s.getCode(), s.getFullName(), s.getPhone(), guardianPhones.get(s.getId()),
                    e.getValue().size(), outstanding, overdue, oldest));
        }
        items.sort(Comparator.comparing(BillingDtos.DebtItem::overdueAmount).thenComparing(BillingDtos.DebtItem::outstanding).reversed());
        return new BillingDtos.DebtSummary(totalOutstanding, totalOverdue, items.size(), items);
    }

    @Transactional(readOnly = true)
    public BillingDtos.VietQrResponse vietQr(Long invoiceId) {
        Invoice inv = findInvoice(invoiceId);
        return qrFor(tenantService.current(), inv);
    }

    public BillingDtos.VietQrResponse qrFor(Tenant tenant, Invoice inv) {
        if (inv.getStatus() == Invoice.Status.CANCELLED || inv.getStatus() == Invoice.Status.PAID) {
            throw new BusinessException("Hóa đơn không còn khoản cần thanh toán");
        }
        Student s = lookups.students(inv.getTenantId(), List.of(inv.getStudentId())).get(inv.getStudentId());
        return vietQrService.build(tenant, inv.remaining(), inv.getCode(), s.getFullName());
    }

    // ---------- Nội bộ ----------

    public Invoice findInvoice(Long id) {
        return invoiceRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("hóa đơn"));
    }

    private FeePlan findPlan(Long id) {
        return feePlanRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("gói học phí"));
    }

    private void applyPlan(FeePlan p, BillingDtos.FeePlanRequest req) {
        if (req.courseId() != null) catalogService.findCourse(req.courseId());
        p.setName(req.name().trim());
        p.setCourseId(req.courseId());
        p.setAmount(req.amount());
        p.setSessions(req.sessions());
        p.setDescription(req.description());
        if (req.active() != null) p.setActive(req.active());
    }

    private String courseName(Long courseId) {
        return courseId == null ? null : lookups.courseNames(CurrentUser.tenantId(), List.of(courseId)).get(courseId);
    }

    private static BillingDtos.FeePlanResponse toPlan(FeePlan p, String courseName) {
        return new BillingDtos.FeePlanResponse(p.getId(), p.getName(), p.getCourseId(), courseName, p.getAmount(),
                p.getSessions(), p.getDescription(), p.isActive());
    }

    BillingDtos.InvoiceResponse detail(Invoice inv) {
        Long tenantId = inv.getTenantId();
        Student s = lookups.students(tenantId, List.of(inv.getStudentId())).get(inv.getStudentId());
        ClassGroup c = inv.getClassId() == null ? null : lookups.classes(tenantId, List.of(inv.getClassId())).get(inv.getClassId());
        List<Payment> payments = paymentRepository.findByTenantIdAndInvoiceIdOrderByPaidAtAsc(tenantId, inv.getId());
        Map<Long, String> receivers = lookups.userNames(tenantId, payments.stream().map(Payment::getReceivedBy).toList());
        List<BillingDtos.PaymentResponse> pr = payments.stream().map(p -> new BillingDtos.PaymentResponse(p.getId(), p.getReceiptNo(),
                p.getAmount(), p.getMethod(), p.getPaidAt(), p.getNote(), receivers.get(p.getReceivedBy()))).toList();
        return toResponse(inv, s, c, pr);
    }

    private BillingDtos.InvoiceResponse toResponse(Invoice i, Student s, ClassGroup c, List<BillingDtos.PaymentResponse> payments) {
        return new BillingDtos.InvoiceResponse(i.getId(), i.getCode(), i.getStudentId(), s == null ? null : s.getCode(),
                s == null ? null : s.getFullName(), i.getClassId(), c == null ? null : c.getCode(), i.getFeePlanId(),
                i.getDescription(), i.getSubtotal(), i.getDiscount(), i.getTotal(), i.getPaidAmount(), i.remaining(),
                i.getDueDate(), i.getStatus(), i.isOverdue(LocalDate.now(clock)), i.getNote(), i.getCreatedAt(), payments);
    }
}
