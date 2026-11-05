package vn.bookeasy.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    private Long bookingId;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private String providerTxnId;
    private long amount;
    private String content;
    private boolean matched;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
