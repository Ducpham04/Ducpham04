package vn.educenter.classes;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "class_sessions")
@Getter
@Setter
public class ClassSession extends TenantEntity {
    public enum Status { SCHEDULED, COMPLETED, CANCELLED }

    @Column(name = "class_id", nullable = false)
    private Long classId;
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(name = "room_id")
    private Long roomId;
    @Column(name = "teacher_id")
    private Long teacherId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SCHEDULED;
    private String note;
}
