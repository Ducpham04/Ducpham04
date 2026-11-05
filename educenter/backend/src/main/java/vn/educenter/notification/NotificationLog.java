package vn.educenter.notification;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
public class NotificationLog {
    public enum Channel { EMAIL, ZALO_ZNS }
    public enum Type { SESSION_REMINDER, FEE_REMINDER }
    public enum Status { SENT, FAILED, MOCK, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(nullable = false)
    private String recipient;
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    private String error;
    @Column(name = "ref_key", nullable = false)
    private String refKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
