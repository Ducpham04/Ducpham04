package vn.educenter.classes;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    Optional<ClassSession> findByIdAndTenantId(Long id, Long tenantId);

    List<ClassSession> findByTenantIdAndClassIdOrderBySessionDateAscStartTimeAsc(Long tenantId, Long classId);

    List<ClassSession> findByTenantIdAndClassIdAndSessionDateGreaterThanEqual(Long tenantId, Long classId, LocalDate from);

    @Query("""
            select s from ClassSession s
            where s.tenantId = :tenantId and s.sessionDate between :from and :to
              and (:teacherId is null or s.teacherId = :teacherId)
              and (:roomId is null or s.roomId = :roomId)
              and (:classId is null or s.classId = :classId)
            order by s.sessionDate, s.startTime
            """)
    List<ClassSession> findRange(Long tenantId, LocalDate from, LocalDate to, Long teacherId, Long roomId, Long classId);

    @Query("""
            select s from ClassSession s
            where s.tenantId = :tenantId and s.sessionDate = :date and s.id <> :excludeId
              and s.status <> vn.educenter.classes.ClassSession.Status.CANCELLED
              and s.startTime < :endTime and s.endTime > :startTime
              and ((:roomId is not null and s.roomId = :roomId) or (:teacherId is not null and s.teacherId = :teacherId))
            """)
    List<ClassSession> findOverlapping(Long tenantId, LocalDate date, java.time.LocalTime startTime,
                                       java.time.LocalTime endTime, Long roomId, Long teacherId, Long excludeId);

    @Query("select s from ClassSession s where s.sessionDate = :date and s.status = vn.educenter.classes.ClassSession.Status.SCHEDULED")
    List<ClassSession> findAllScheduledOn(LocalDate date);

    @Query("select s from ClassSession s where s.tenantId = :tenantId and s.classId in :classIds and s.sessionDate between :from and :to and s.status <> vn.educenter.classes.ClassSession.Status.CANCELLED order by s.sessionDate, s.startTime")
    List<ClassSession> findForClasses(Long tenantId, Collection<Long> classIds, LocalDate from, LocalDate to);

    long countByTenantIdAndSessionDate(Long tenantId, LocalDate date);

    @Modifying
    @Query("delete from ClassSession s where s.id in :ids")
    void deleteByIds(Collection<Long> ids);
}
