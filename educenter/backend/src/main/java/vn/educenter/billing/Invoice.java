package vn.educenter.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice extends TenantEntity {
    public enum Status { UNPAID, PARTIAL, PAID, CANCELLED }

    @Column(nullable = false)
    private String code;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "class_id")
    private Long classId;
    @Column(name = "fee_plan_id")
    private Long feePlanId;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private BigDecimal subtotal;
    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal total;
    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UNPAID;
    private String note;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "last_reminded_at")
    private Instant lastRemindedAt;

    public BigDecimal remaining() {
        return InvoiceMath.remaining(total, paidAmount);
    }

    public boolean isOverdue(LocalDate today) {
        return InvoiceMath.isOverdue(status, dueDate, today);
    }
}
