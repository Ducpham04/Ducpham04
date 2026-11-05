package vn.educenter.student;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    List<Guardian> findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(Long tenantId, Long studentId);

    List<Guardian> findByTenantIdAndStudentIdIn(Long tenantId, Collection<Long> studentIds);

    Optional<Guardian> findByIdAndTenantId(Long id, Long tenantId);
}
