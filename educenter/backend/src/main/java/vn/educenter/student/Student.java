package vn.educenter.student;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "students")
@Getter
@Setter
public class Student extends TenantEntity {
    public enum Status { TRIAL, ACTIVE, ON_HOLD, DROPPED }
    public enum Gender { MALE, FEMALE, OTHER }

    @Column(nullable = false)
    private String code;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private LocalDate dob;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    private String phone;
    private String email;
    private String school;
    private String level;
    private String address;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;
    private String note;
}
