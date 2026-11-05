package vn.educenter.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTenantIdOrderByNameAsc(Long tenantId);

    Optional<Course> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);

    List<Course> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);
}
