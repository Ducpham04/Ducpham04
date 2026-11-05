package vn.educenter.student;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "guardians")
@Getter
@Setter
public class Guardian extends TenantEntity {
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String relationship;
    private String phone;
    private String email;
    @Column(name = "zalo_phone")
    private String zaloPhone;
    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
