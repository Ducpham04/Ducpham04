package vn.educenter.tenant;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsBySlug(String slug);

    List<Tenant> findByStatus(Tenant.Status status);

    List<Tenant> findAllByOrderByCreatedAtDesc();
}
