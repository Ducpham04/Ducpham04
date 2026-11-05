package vn.educenter.attendance;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attendances")
@Getter
@Setter
public class Attendance {
    public enum Status { PRESENT, LATE, EXCUSED, ABSENT;
        public boolean attended() { return this == PRESENT || this == LATE; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    private String note;
    @Column(name = "marked_by")
    private Long markedBy;
    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;
}
