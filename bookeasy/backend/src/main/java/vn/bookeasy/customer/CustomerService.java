package vn.bookeasy.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.bookeasy.booking.Booking;
import vn.bookeasy.booking.BookingRepository;
import vn.bookeasy.booking.BookingStatus;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.customer.CustomerDtos.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CustomerService {
    private final CustomerRepository customers;
    private final CustomerQueryRepository customerQuery;
    private final BookingRepository bookings;
    private final TransactionTemplate newTx;

    public CustomerService(CustomerRepository customers, CustomerQueryRepository customerQuery,
                           BookingRepository bookings, PlatformTransactionManager txm) {
        this.customers = customers;
        this.customerQuery = customerQuery;
        this.bookings = bookings;
        this.newTx = new TransactionTemplate(txm);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Tìm theo SĐT hoặc tạo mới (giao dịch riêng, chịu được tạo đồng thời). Trả về id khách. */
    public Long upsert(Long bid, String fullName, String phone, String email) {
        try {
            return newTx.execute(st -> doUpsert(bid, fullName, phone, email));
        } catch (DataIntegrityViolationException e) {
            return newTx.execute(st -> doUpsert(bid, fullName, phone, email));
        }
    }

    private Long doUpsert(Long bid, String fullName, String phone, String email) {
        Customer c = customers.findByBusinessIdAndPhone(bid, phone).orElseGet(() -> {
            Customer n = new Customer();
            n.setBusinessId(bid);
            n.setPhone(phone);
            return n;
        });
        c.setFullName(fullName.trim());
        if (TextUtils.blankToNull(email) != null) c.setEmail(email.trim().toLowerCase(Locale.ROOT));
        return customers.saveAndFlush(c).getId();
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummary> search(Long bid, String q, int page, int size) {
        String like = TextUtils.blankToNull(q) == null ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Page<Customer> p = customerQuery.search(bid, like, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        Map<Long, Stats> stats = stats(p.getContent().stream().map(Customer::getId).toList());
        List<CustomerSummary> items = p.getContent().stream()
                .map(c -> toSummary(c, stats.getOrDefault(c.getId(), new Stats()))).toList();
        return new PageResponse<>(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CustomerDetail get(Long bid, Long id) {
        Customer c = customers.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("khách hàng"));
        Stats s = stats(List.of(id)).getOrDefault(id, new Stats());
        List<CustomerBooking> history = bookings.findByCustomer(id).stream().map(CustomerService::toHistory).toList();
        return new CustomerDetail(toSummary(c, s), c.getNotes(), history);
    }

    @Transactional
    public CustomerDetail update(Long bid, Long id, UpdateCustomerRequest req) {
        Customer c = customers.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("khách hàng"));
        if (req.fullName() != null && !req.fullName().isBlank()) c.setFullName(req.fullName().trim());
        if (req.email() != null) c.setEmail(TextUtils.blankToNull(req.email()));
        if (req.notes() != null) c.setNotes(TextUtils.blankToNull(req.notes()));
        return get(bid, id);
    }

    private static final class Stats {
        long visits;
        long noShows;
        long cancelled;
        long total;
        long spent;
        LocalDateTime lastVisit;
    }

    private Map<Long, Stats> stats(Collection<Long> ids) {
        Map<Long, Stats> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (Object[] row : bookings.customerStats(ids)) {
            Long cid = (Long) row[0];
            BookingStatus status = (BookingStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long sum = row[3] == null ? 0 : ((Number) row[3]).longValue();
            LocalDateTime last = (LocalDateTime) row[4];
            Stats s = map.computeIfAbsent(cid, k -> new Stats());
            s.total += count;
            switch (status) {
                case COMPLETED -> {
                    s.visits += count;
                    s.spent += sum;
                    if (s.lastVisit == null || last.isAfter(s.lastVisit)) s.lastVisit = last;
                }
                case NO_SHOW -> s.noShows += count;
                case CANCELLED -> s.cancelled += count;
                default -> {
                }
            }
        }
        return map;
    }

    private static CustomerSummary toSummary(Customer c, Stats s) {
        return new CustomerSummary(c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), s.total, s.visits,
                s.noShows, s.cancelled, s.spent, s.lastVisit, c.getCreatedAt());
    }

    private static CustomerBooking toHistory(Booking b) {
        return new CustomerBooking(b.getId(), b.getCode(), b.getStartAt(), b.getStatus(), b.getService().getName(),
                b.getStaff().getFullName(), b.getPrice());
    }
}
