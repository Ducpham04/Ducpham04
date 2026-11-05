package vn.educenter.user;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.BaseEntity;
import vn.educenter.security.Role;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    @Column(name = "tenant_id")
    private Long tenantId;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(name = "student_id")
    private Long studentId;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
