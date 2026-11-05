package vn.educenter.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import vn.educenter.common.TenantEntity;

@Entity
@Table(name = "fee_plans")
@Getter
@Setter
public class FeePlan extends TenantEntity {
    @Column(nullable = false)
    private String name;
    @Column(name = "course_id")
    private Long courseId;
    @Column(nullable = false)
    private BigDecimal amount;
    private Integer sessions;
    private String description;
    @Column(nullable = false)
    private boolean active = true;
}
