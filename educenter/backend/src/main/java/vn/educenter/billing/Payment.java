package vn.educenter.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {
    public enum Method { CASH, BANK_TRANSFER, CARD, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
    @Column(name = "receipt_no", nullable = false)
    private String receiptNo;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;
    private String note;
    @Column(name = "received_by")
    private Long receivedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
