package vn.bookeasy.business;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
public class Business {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;
    @Column(nullable = false)
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String logoUrl;
    @Column(nullable = false)
    private String brandColor = "#2f7d6b";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessCategory category = BusinessCategory.SPA;
    @Column(nullable = false)
    private String timezone = "Asia/Ho_Chi_Minh";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessStatus status = BusinessStatus.ACTIVE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode = PaymentMode.NONE;
    private int depositPercent = 30;
    private String bankBin;
    private String bankAccountNo;
    private String bankAccountName;
    private int minLeadMinutes = 60;
    private int bookingWindowDays = 14;
    private int cancelBeforeMinutes = 120;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean paymentConfigured() {
        return paymentMode != PaymentMode.NONE && bankBin != null && bankAccountNo != null;
    }
}
