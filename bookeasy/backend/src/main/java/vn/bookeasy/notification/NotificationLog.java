package vn.bookeasy.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;
    private Long bookingId;
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private String type;
    private String recipient;
    @Column(nullable = false)
    private String status;
    private String error;
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
