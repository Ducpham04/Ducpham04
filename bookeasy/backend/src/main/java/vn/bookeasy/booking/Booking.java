package vn.bookeasy.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    @Column(nullable = false, unique = true)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private vn.bookeasy.business.Branch branch;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private vn.bookeasy.customer.Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id")
    private vn.bookeasy.staff.Staff staff;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id")
    private vn.bookeasy.catalog.ServiceItem service;
    @Column(nullable = false)
    private LocalDateTime startAt;
    @Column(nullable = false)
    private LocalDateTime endAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingSource source = BookingSource.ONLINE;
    private long price;
    private long amountDue;
    private long amountPaid;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;
    private LocalDateTime paymentExpiresAt;
    private String customerNote;
    private String internalNote;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    @Column(name = "reminder_24h_sent_at")
    private LocalDateTime reminder24hSentAt;
    @Column(name = "reminder_2h_sent_at")
    private LocalDateTime reminder2hSentAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
