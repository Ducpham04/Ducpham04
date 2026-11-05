package vn.educenter.billing;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id and i.tenantId = :tenantId")
    Optional<Invoice> lockByIdAndTenantId(Long id, Long tenantId);

    List<Invoice> findByTenantIdAndStudentIdOrderByCreatedAtDesc(Long tenantId, Long studentId);

    @Query("""
            select i from Invoice i where i.tenantId = :tenantId
              and (:studentId is null or i.studentId = :studentId)
              and i.status in :statuses
              and (:overdueBefore is null or i.dueDate < :overdueBefore)
              and (:q is null or lower(i.code) like lower(concat('%', :q, '%'))
                   or i.studentId in (select s.id from vn.educenter.student.Student s where s.tenantId = :tenantId
                                      and lower(s.fullName) like lower(concat('%', :q, '%'))))
            """)
    Page<Invoice> search(Long tenantId, Long studentId, Collection<Invoice.Status> statuses, LocalDate overdueBefore,
                         String q, Pageable pageable);

    @Query("select i from Invoice i where i.tenantId = :tenantId and i.status in ('UNPAID','PARTIAL') order by i.dueDate")
    List<Invoice> findOpen(Long tenantId);

    @Query("select coalesce(sum(i.total - i.paidAmount), 0) from Invoice i where i.tenantId = :tenantId and i.status in ('UNPAID','PARTIAL')")
    BigDecimal sumOutstanding(Long tenantId);

    @Query("select coalesce(sum(i.total - i.paidAmount), 0) from Invoice i where i.tenantId = :tenantId and i.status in ('UNPAID','PARTIAL') and i.dueDate < :today")
    BigDecimal sumOverdue(Long tenantId, LocalDate today);

    /** Hóa đơn cần nhắc: chưa thu đủ, hạn <= ngưỡng, chưa nhắc từ đầu ngày. */
    @Query("""
            select i from Invoice i where i.tenantId = :tenantId and i.status in ('UNPAID','PARTIAL')
              and i.dueDate <= :dueBefore and (i.lastRemindedAt is null or i.lastRemindedAt < :remindedBefore)
            """)
    List<Invoice> findToRemind(Long tenantId, LocalDate dueBefore, java.time.Instant remindedBefore);
}
