package vn.educenter.classes;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
public class Enrollment extends TenantEntity {
    public enum Status { ACTIVE, COMPLETED, DROPPED }

    @Column(name = "class_id", nullable = false)
    private Long classId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}
