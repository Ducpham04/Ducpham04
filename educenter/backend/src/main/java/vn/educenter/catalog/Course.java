package vn.educenter.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course extends TenantEntity {
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    private String level;
    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;
    @Column(name = "tuition_fee", nullable = false)
    private BigDecimal tuitionFee = BigDecimal.ZERO;
    private String description;
    @Column(nullable = false)
    private boolean active = true;
}
