package vn.educenter.student;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByIdAndTenantId(Long id, Long tenantId);

    List<Student> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, Student.Status status);

    long countByTenantIdAndJoinedAtBetween(Long tenantId, LocalDate from, LocalDate to);

    @Query("""
            select s from Student s
            where s.tenantId = :tenantId
              and (:status is null or s.status = :status)
              and (:q is null or lower(s.fullName) like lower(concat('%', :q, '%'))
                   or s.phone like concat('%', :q, '%') or lower(s.code) like lower(concat('%', :q, '%')))
            """)
    Page<Student> search(Long tenantId, String q, Student.Status status, Pageable pageable);

    @Query("select s.joinedAt from Student s where s.tenantId = :tenantId and s.joinedAt between :from and :to")
    List<LocalDate> joinedDates(Long tenantId, LocalDate from, LocalDate to);
}
