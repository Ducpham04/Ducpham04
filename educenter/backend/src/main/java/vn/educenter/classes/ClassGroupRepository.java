package vn.educenter.classes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    Optional<ClassGroup> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(Long tenantId, String code, Long id);

    List<ClassGroup> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);

    List<ClassGroup> findByTenantIdAndStatusIn(Long tenantId, Collection<ClassGroup.Status> statuses);

    @Query("""
            select c from ClassGroup c
            where c.tenantId = :tenantId
              and (:status is null or c.status = :status)
              and (:teacherId is null or c.teacherId = :teacherId)
              and (:q is null or lower(c.name) like lower(concat('%', :q, '%')) or lower(c.code) like lower(concat('%', :q, '%')))
            order by case c.status when 'ONGOING' then 0 when 'PLANNED' then 1 when 'FINISHED' then 2 else 3 end, c.startDate desc
            """)
    List<ClassGroup> search(Long tenantId, String q, ClassGroup.Status status, Long teacherId);

    boolean existsByTenantIdAndCourseId(Long tenantId, Long courseId);
}
