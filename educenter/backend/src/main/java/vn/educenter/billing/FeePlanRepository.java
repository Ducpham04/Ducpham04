package vn.educenter.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePlanRepository extends JpaRepository<FeePlan, Long> {
    List<FeePlan> findByTenantIdOrderByNameAsc(Long tenantId);

    Optional<FeePlan> findByIdAndTenantId(Long id, Long tenantId);
}
