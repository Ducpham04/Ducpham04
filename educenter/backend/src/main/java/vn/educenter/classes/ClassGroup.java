package vn.educenter.classes;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "classes")
@Getter
@Setter
public class ClassGroup extends TenantEntity {
    public enum Status { PLANNED, ONGOING, FINISHED, CANCELLED;
        public boolean isOpen() { return this == PLANNED || this == ONGOING; }
    }

    @Column(name = "course_id", nullable = false)
    private Long courseId;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "teacher_id")
    private Long teacherId;
    @Column(name = "room_id")
    private Long roomId;
    @Column(name = "max_students", nullable = false)
    private int maxStudents;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PLANNED;
    private String note;
}
