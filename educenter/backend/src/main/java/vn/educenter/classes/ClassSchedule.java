package vn.educenter.classes;

import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "class_schedules")
@Getter
@Setter
public class ClassSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "class_id", nullable = false)
    private Long classId;
    /** 1 = Thứ 2 ... 7 = Chủ nhật (ISO-8601). */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
