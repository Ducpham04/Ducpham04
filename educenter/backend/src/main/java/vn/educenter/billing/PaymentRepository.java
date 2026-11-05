package vn.educenter.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTenantIdAndInvoiceIdOrderByPaidAtAsc(Long tenantId, Long invoiceId);

    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByInvoiceId(Long invoiceId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.tenantId = :tenantId and p.paidAt >= :from and p.paidAt < :to")
    BigDecimal sumBetween(Long tenantId, Instant from, Instant to);

    @Query("select p.paidAt, p.amount from Payment p where p.tenantId = :tenantId and p.paidAt >= :from and p.paidAt < :to")
    List<Object[]> amountsBetween(Long tenantId, Instant from, Instant to);
}
