package vn.bookeasy.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.bookeasy.auth.AuthPrincipal;
import vn.bookeasy.booking.BookingDtos.*;
import vn.bookeasy.business.*;
import vn.bookeasy.catalog.ServiceItem;
import vn.bookeasy.catalog.ServiceItemRepository;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.config.AppProperties;
import vn.bookeasy.customer.CustomerRepository;
import vn.bookeasy.customer.CustomerService;
import vn.bookeasy.payment.VietQrService;
import vn.bookeasy.staff.Staff;
import vn.bookeasy.staff.StaffRepository;

import java.time.*;
import java.util.*;

@Slf4j
@Service
public class BookingService {
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;
    private final BusinessRepository businesses;
    private final BranchRepository branches;
    private final ServiceItemRepository services;
    private final StaffRepository staffRepo;
    private final CustomerRepository customers;
    private final CustomerService customerService;
    private final AvailabilityService availability;
    private final BookingCodeGenerator codes;
    private final VietQrService vietQr;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public BookingService(BookingRepository bookings, BookingSlotRepository slots, BusinessRepository businesses,
                          BranchRepository branches, ServiceItemRepository services, StaffRepository staffRepo,
                          CustomerRepository customers, CustomerService customerService,
                          AvailabilityService availability, BookingCodeGenerator codes, VietQrService vietQr,
                          ApplicationEventPublisher events, AppProperties props, Clock clock,
                          PlatformTransactionManager txm) {
        this.bookings = bookings;
        this.slots = slots;
        this.businesses = businesses;
        this.branches = branches;
        this.services = services;
        this.staffRepo = staffRepo;
        this.customers = customers;
        this.customerService = customerService;
        this.availability = availability;
        this.codes = codes;
        this.vietQr = vietQr;
        this.events = events;
        this.props = props;
        this.clock = clock;
        this.tx = new TransactionTemplate(txm);
    }

    // =============== Công khai ===============

    public Business requireActiveBusiness(String slug) {
        Business b = businesses.findBySlug(slug).orElseThrow(() -> ApiException.notFound("cơ sở"));
        if (b.getStatus() != BusinessStatus.ACTIVE) {
            throw ApiException.forbidden("Cơ sở đang tạm ngưng nhận lịch trực tuyến");
        }
        return b;
    }

    public PublicBookingResponse createPublic(String slug, PublicBookingRequest req) {
        Business b = requireActiveBusiness(slug);
        Branch branch = activeBranch(b.getId(), req.branchId());
        ServiceItem svc = services.findByIdAndBusinessId(req.serviceId(), b.getId())
                .filter(ServiceItem::isActive).orElseThrow(() -> ApiException.notFound("dịch vụ"));
        String phone = requirePhone(req.phone());
        LocalDateTime start = req.date().atTime(req.time());
        if (!SlotCalculator.isAligned(start)) throw ApiException.badRequest("INVALID_TIME", "Giờ đặt không hợp lệ");

        // Kiểm tra mềm: giờ phải nằm trong lịch trống thực tế (giờ mở cửa ∩ ca ∩ ngày nghỉ ∩ thời gian đặt trước).
        List<AvailabilityService.SlotOption> options = availability.slotsForDay(b, branch.getId(), svc, req.staffId(), req.date());
        List<Long> free = options.stream().filter(o -> o.time().equals(req.time())).findFirst()
                .map(AvailabilityService.SlotOption::staffIds).orElse(List.of());
        if (free.isEmpty()) throw slotTaken();

        List<Long> ordered = free.size() == 1 ? free : orderByLoad(free, req.date());
        Long customerId = customerService.upsert(b.getId(), req.fullName(), phone, req.email());

        for (Long staffId : ordered) {
            try {
                Long id = tx.execute(st -> insert(b, branch, svc, staffId, customerId, start, BookingSource.ONLINE,
                        TextUtils.blankToNull(req.note())));
                return tx.execute(st -> toPublic(bookings.findDetailed(id, b.getId()).orElseThrow()));
            } catch (DataIntegrityViolationException e) {
                log.info("Slot conflict for staff {} at {}, trying next", staffId, start);
            }
        }
        throw slotTaken();
    }

    @Transactional(readOnly = true)
    public PublicBookingResponse getPublic(String code, String phone) {
        return toPublic(requireOwnedByPhone(code, phone));
    }

    @Transactional
    public PublicBookingResponse cancelPublic(String code, PublicCancelRequest req) {
        Booking bk = requireOwnedByPhone(code, req.phone());
        if (!isCancellableByCustomer(bk)) {
            throw ApiException.badRequest("CANNOT_CANCEL", "Lịch hẹn không thể hủy trực tuyến, vui lòng gọi cho cơ sở");
        }
        cancel(bk, TextUtils.blankToNull(req.reason()) == null ? "Khách hủy trực tuyến" : req.reason().trim());
        return toPublic(bk);
    }

    private Booking requireOwnedByPhone(String code, String rawPhone) {
        String phone = TextUtils.normalizePhone(rawPhone);
        return bookings.findByCodeDetailed(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .filter(bk -> phone != null && phone.equals(bk.getCustomer().getPhone()))
                .orElseThrow(() -> ApiException.notFound("lịch hẹn với mã và số điện thoại này"));
    }

    private boolean isCancellableByCustomer(Booking bk) {
        if (!(bk.getStatus() == BookingStatus.CONFIRMED || bk.getStatus() == BookingStatus.PENDING_PAYMENT)) return false;
        Business b = businesses.findById(bk.getBusinessId()).orElseThrow();
        return LocalDateTime.now(clock).plusMinutes(b.getCancelBeforeMinutes()).isBefore(bk.getStartAt());
    }

    // =============== Quản trị ===============

    @Transactional(readOnly = true)
    public List<BookingResponse> search(AuthPrincipal user, LocalDateTime from, LocalDateTime to, Long staffId,
                                        Long branchId, BookingStatus status, String q) {
        if (Duration.between(from, to).toDays() > 62) {
            throw ApiException.badRequest("RANGE_TOO_LARGE", "Khoảng thời gian tối đa 62 ngày");
        }
        Long effectiveStaff = user.isStaff() ? user.staffId() : staffId;
        String like = TextUtils.blankToNull(q) == null ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Business b = businesses.findById(user.businessId()).orElseThrow();
        return bookings.search(user.businessId(), from, to, effectiveStaff, branchId, status, like)
                .stream().map(bk -> toResponse(b, bk)).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(AuthPrincipal user, Long id) {
        Booking bk = requireAccessible(user, id);
        return toResponse(businesses.findById(user.businessId()).orElseThrow(), bk);
    }

    public BookingResponse createAdmin(AuthPrincipal user, AdminBookingRequest req) {
        Long bid = user.businessId();
        if (user.isStaff() && !req.staffId().equals(user.staffId())) {
            throw ApiException.forbidden("Nhân viên chỉ tạo lịch hẹn cho chính mình");
        }
        if (!SlotCalculator.isAligned(req.startAt())) throw ApiException.badRequest("INVALID_TIME", "Giờ phải theo bước 15 phút");
        Business b = businesses.findById(bid).orElseThrow();
        ServiceItem svc = services.findWithStaff(req.serviceId(), bid).orElseThrow(() -> ApiException.notFound("dịch vụ"));
        Staff staff = staffRepo.findByIdAndBusinessId(req.staffId(), bid).filter(Staff::isActive)
                .orElseThrow(() -> ApiException.notFound("nhân viên"));
        Branch branch = activeBranch(bid, req.branchId() == null ? staff.getBranchId() : req.branchId());
        String phone = requirePhone(req.phone());
        Long customerId = customerService.upsert(bid, req.fullName(), phone, req.email());
        BookingSource source = req.source() == null ? BookingSource.ADMIN : req.source();
        try {
            Long id = tx.execute(st -> insert(b, branch, svc, staff.getId(), customerId, req.startAt(), source,
                    TextUtils.blankToNull(req.note())));
            return tx.execute(st -> toResponse(b, bookings.findDetailed(id, bid).orElseThrow()));
        } catch (DataIntegrityViolationException e) {
            throw slotTaken();
        }
    }

    public BookingResponse reschedule(AuthPrincipal user, Long id, RescheduleRequest req) {
        if (!SlotCalculator.isAligned(req.startAt())) throw ApiException.badRequest("INVALID_TIME", "Giờ phải theo bước 15 phút");
        try {
            return tx.execute(st -> {
                Booking bk = requireAccessible(user, id);
                if (!bk.getStatus().isActive() || bk.getStatus() == BookingStatus.CHECKED_IN) {
                    throw ApiException.badRequest("CANNOT_RESCHEDULE", "Chỉ đổi giờ được lịch hẹn chưa diễn ra");
                }
                Staff target = bk.getStaff();
                if (req.staffId() != null && !req.staffId().equals(target.getId())) {
                    if (user.isStaff()) throw ApiException.forbidden("Nhân viên không thể chuyển lịch cho người khác");
                    target = staffRepo.findByIdAndBusinessId(req.staffId(), user.businessId()).filter(Staff::isActive)
                            .orElseThrow(() -> ApiException.notFound("nhân viên"));
                }
                int duration = bk.getService().getDurationMinutes();
                slots.deleteByBookingId(bk.getId());
                slots.flush();
                bk.setStaff(target);
                bk.setStartAt(req.startAt());
                bk.setEndAt(req.startAt().plusMinutes(duration));
                bk.setReminder24hSentAt(null);
                bk.setReminder2hSentAt(null);
                lockSlots(bk.getId(), target.getId(), req.startAt(), duration);
                bookings.flush();
                events.publishEvent(new BookingEvent(bk.getId(), BookingEvent.Type.RESCHEDULED));
                return toResponse(businesses.findById(user.businessId()).orElseThrow(), bk);
            });
        } catch (DataIntegrityViolationException e) {
            throw slotTaken();
        }
    }

    @Transactional
    public BookingResponse changeStatus(AuthPrincipal user, Long id, StatusRequest req) {
        Booking bk = requireAccessible(user, id);
        BookingStatus target = req.status();
        if (user.isStaff() && !EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.COMPLETED, BookingStatus.NO_SHOW).contains(target)) {
            throw ApiException.forbidden("Nhân viên chỉ được check-in, hoàn thành hoặc đánh dấu không đến");
        }
        if (!bk.getStatus().canTransitionTo(target)) {
            throw ApiException.badRequest("INVALID_TRANSITION", "Không thể chuyển trạng thái từ "
                    + label(bk.getStatus()) + " sang " + label(target));
        }
        if (target == BookingStatus.NO_SHOW && LocalDateTime.now(clock).isBefore(bk.getStartAt())) {
            throw ApiException.badRequest("TOO_EARLY", "Chỉ đánh dấu không đến sau giờ hẹn");
        }
        switch (target) {
            case CANCELLED -> cancel(bk, TextUtils.blankToNull(req.reason()) == null ? "Cơ sở hủy" : req.reason().trim());
            case CONFIRMED -> confirm(bk);
            default -> bk.setStatus(target);
        }
        return toResponse(businesses.findById(user.businessId()).orElseThrow(), bk);
    }

    @Transactional
    public BookingResponse updateNote(AuthPrincipal user, Long id, NoteRequest req) {
        Booking bk = requireAccessible(user, id);
        bk.setInternalNote(TextUtils.blankToNull(req.internalNote()));
        return toResponse(businesses.findById(user.businessId()).orElseThrow(), bk);
    }

    @Transactional
    public BookingResponse markPaid(AuthPrincipal user, Long id) {
        Booking bk = requireAccessible(user, id);
        applyPayment(bk, Math.max(bk.getAmountDue() - bk.getAmountPaid(), 0));
        return toResponse(businesses.findById(user.businessId()).orElseThrow(), bk);
    }

    // =============== Dùng chung (thanh toán, scheduler) ===============

    /** Ghi nhận tiền vào booking; đủ số tiền cần thu → xác nhận booking. */
    public void applyPayment(Booking bk, long amount) {
        if (bk.getPaymentStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("NO_PAYMENT_PENDING", "Lịch hẹn không có khoản thanh toán đang chờ");
        }
        bk.setAmountPaid(bk.getAmountPaid() + amount);
        if (bk.getAmountPaid() >= bk.getAmountDue()) {
            bk.setPaymentStatus(PaymentStatus.PAID);
            bk.setPaymentExpiresAt(null);
            if (bk.getStatus() == BookingStatus.PENDING_PAYMENT) confirm(bk);
        }
    }

    @Transactional
    public void expire(Long bookingId) {
        bookings.findById(bookingId).ifPresent(bk -> {
            if (bk.getStatus() == BookingStatus.PENDING_PAYMENT) cancel(bk, "Quá hạn thanh toán");
        });
    }

    private void confirm(Booking bk) {
        bk.setStatus(BookingStatus.CONFIRMED);
        events.publishEvent(new BookingEvent(bk.getId(), BookingEvent.Type.CONFIRMED));
    }

    private void cancel(Booking bk, String reason) {
        bk.setStatus(BookingStatus.CANCELLED);
        bk.setCancelReason(reason);
        bk.setCancelledAt(LocalDateTime.now(clock));
        if (bk.getPaymentStatus() == PaymentStatus.PENDING) bk.setPaymentExpiresAt(null);
        slots.deleteByBookingId(bk.getId());
        events.publishEvent(new BookingEvent(bk.getId(), BookingEvent.Type.CANCELLED));
    }

    private Long insert(Business b, Branch branch, ServiceItem svc, Long staffId, Long customerId,
                        LocalDateTime start, BookingSource source, String note) {
        if (start.isBefore(LocalDateTime.now(clock).minusHours(12))) {
            throw ApiException.badRequest("PAST_TIME", "Không thể đặt lịch trong quá khứ");
        }
        Booking bk = new Booking();
        bk.setBusinessId(b.getId());
        bk.setCode(codes.next());
        bk.setBranch(branch);
        bk.setService(svc);
        bk.setStaff(staffRepo.getReferenceById(staffId));
        bk.setCustomer(customers.getReferenceById(customerId));
        bk.setStartAt(start);
        bk.setEndAt(start.plusMinutes(svc.getDurationMinutes()));
        bk.setPrice(svc.getPrice());
        bk.setSource(source);
        bk.setCustomerNote(note);
        long due = source == BookingSource.ONLINE ? amountDue(b, svc.getPrice()) : 0;
        if (due > 0) {
            bk.setAmountDue(due);
            bk.setPaymentStatus(PaymentStatus.PENDING);
            bk.setStatus(BookingStatus.PENDING_PAYMENT);
            bk.setPaymentExpiresAt(LocalDateTime.now(clock).plusMinutes(props.payment().holdMinutes()));
        } else {
            bk.setStatus(BookingStatus.CONFIRMED);
        }
        bookings.saveAndFlush(bk);
        lockSlots(bk.getId(), staffId, start, svc.getDurationMinutes());
        if (bk.getStatus() == BookingStatus.CONFIRMED) {
            events.publishEvent(new BookingEvent(bk.getId(), BookingEvent.Type.CONFIRMED));
        }
        return bk.getId();
    }

    /** Khóa slot ở tầng DB – UNIQUE(staff_id, slot_start) ném DataIntegrityViolationException nếu trùng. */
    private void lockSlots(Long bookingId, Long staffId, LocalDateTime start, int duration) {
        List<BookingSlot> list = SlotCalculator.units(start, duration).stream()
                .map(u -> new BookingSlot(bookingId, staffId, u)).toList();
        slots.saveAllAndFlush(list);
    }

    static long amountDue(Business b, long price) {
        if (!b.paymentConfigured() || price <= 0) return 0;
        if (b.getPaymentMode() == PaymentMode.FULL) return price;
        long raw = price * b.getDepositPercent() / 100;
        return Math.min(price, ((raw + 999) / 1000) * 1000);
    }

    private List<Long> orderByLoad(List<Long> staffIds, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        return staffIds.stream()
                .sorted(Comparator.comparingLong((Long id) -> bookings.countForStaff(id, from, to, BookingStatus.ACTIVE))
                        .thenComparing(id -> id))
                .toList();
    }

    private Booking requireAccessible(AuthPrincipal user, Long id) {
        Booking bk = bookings.findDetailed(id, user.businessId()).orElseThrow(() -> ApiException.notFound("lịch hẹn"));
        if (user.isStaff() && !bk.getStaff().getId().equals(user.staffId())) throw ApiException.notFound("lịch hẹn");
        return bk;
    }

    private Branch activeBranch(Long bid, Long branchId) {
        return branches.findByIdAndBusinessId(branchId, bid).filter(Branch::isActive)
                .orElseThrow(() -> ApiException.notFound("chi nhánh"));
    }

    private static String requirePhone(String raw) {
        String phone = TextUtils.normalizePhone(raw);
        if (phone == null) throw ApiException.badRequest("INVALID_PHONE", "Số điện thoại không hợp lệ");
        return phone;
    }

    private static ApiException slotTaken() {
        return ApiException.conflict("SLOT_TAKEN", "Khung giờ vừa có người đặt, vui lòng chọn giờ khác");
    }

    static String label(BookingStatus s) {
        return switch (s) {
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case CONFIRMED -> "Đã xác nhận";
            case CHECKED_IN -> "Đã đến";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
            case NO_SHOW -> "Không đến";
        };
    }

    // =============== Mapping ===============

    PublicBookingResponse toPublic(Booking bk) {
        Business b = businesses.findById(bk.getBusinessId()).orElseThrow();
        return new PublicBookingResponse(bk.getCode(), bk.getStatus(), b.getName(), b.getSlug(), bk.getBranch().getName(),
                bk.getBranch().getAddress(), bk.getService().getName(), bk.getService().getDurationMinutes(),
                bk.getStaff().getFullName(), bk.getStartAt(), bk.getEndAt(), bk.getPrice(), bk.getAmountDue(),
                bk.getAmountPaid(), bk.getPaymentStatus(), bk.getPaymentExpiresAt(), vietQr.paymentInfo(b, bk),
                bk.getCustomer().getFullName(), isCancellableByCustomer(bk));
    }

    BookingResponse toResponse(Business b, Booking bk) {
        return new BookingResponse(bk.getId(), bk.getCode(), bk.getStatus(), bk.getSource(), bk.getStartAt(),
                bk.getEndAt(), bk.getPrice(), bk.getAmountDue(), bk.getAmountPaid(), bk.getPaymentStatus(),
                bk.getPaymentExpiresAt(),
                new CustomerRef(bk.getCustomer().getId(), bk.getCustomer().getFullName(), bk.getCustomer().getPhone(),
                        bk.getCustomer().getEmail()),
                new StaffRef(bk.getStaff().getId(), bk.getStaff().getFullName(), bk.getStaff().getColor()),
                new ServiceRef(bk.getService().getId(), bk.getService().getName(), bk.getService().getDurationMinutes()),
                new Ref(bk.getBranch().getId(), bk.getBranch().getName()),
                bk.getCustomerNote(), bk.getInternalNote(), bk.getCancelReason(), bk.getCreatedAt(),
                vietQr.paymentInfo(b, bk));
    }
}
