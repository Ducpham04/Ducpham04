package vn.educenter.classes;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    java.util.Optional<Enrollment> findByIdAndTenantId(Long id, Long tenantId);

    List<Enrollment> findByTenantIdAndClassIdOrderByStatusAscIdAsc(Long tenantId, Long classId);

    List<Enrollment> findByTenantIdAndClassIdAndStatus(Long tenantId, Long classId, Enrollment.Status status);

    List<Enrollment> findByTenantIdAndStudentIdOrderByStartDateDesc(Long tenantId, Long studentId);

    List<Enrollment> findByTenantIdAndStudentIdAndStatus(Long tenantId, Long studentId, Enrollment.Status status);

    boolean existsByTenantIdAndClassIdAndStudentIdAndStatus(Long tenantId, Long classId, Long studentId, Enrollment.Status status);

    long countByTenantIdAndClassIdAndStatus(Long tenantId, Long classId, Enrollment.Status status);

    @Query("select e.classId, count(e) from Enrollment e where e.tenantId = :tenantId and e.status = 'ACTIVE' and e.classId in :classIds group by e.classId")
    List<Object[]> countActiveByClass(Long tenantId, Collection<Long> classIds);

    @Query("select e from Enrollment e where e.tenantId = :tenantId and e.status = 'ACTIVE' and e.classId in :classIds")
    List<Enrollment> findActiveByClassIds(Long tenantId, Collection<Long> classIds);
}
