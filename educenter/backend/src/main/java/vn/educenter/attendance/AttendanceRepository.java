package vn.educenter.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByTenantIdAndSessionId(Long tenantId, Long sessionId);

    boolean existsBySessionId(Long sessionId);

    @Query("select a.sessionId from Attendance a where a.sessionId in :sessionIds group by a.sessionId")
    List<Long> sessionIdsWithAttendance(Collection<Long> sessionIds);

    @Query("select a.sessionId, count(a) from Attendance a where a.tenantId = :tenantId and a.sessionId in :sessionIds group by a.sessionId")
    List<Object[]> countBySession(Long tenantId, Collection<Long> sessionIds);

    /** [studentId, status, count] trong 1 lớp. */
    @Query("""
            select a.studentId, a.status, count(a) from Attendance a, vn.educenter.classes.ClassSession s
            where s.id = a.sessionId and a.tenantId = :tenantId and s.classId = :classId
            group by a.studentId, a.status
            """)
    List<Object[]> statsByStudentInClass(Long tenantId, Long classId);

    /** [classId, status, count] trong khoảng ngày. */
    @Query("""
            select s.classId, a.status, count(a) from Attendance a, vn.educenter.classes.ClassSession s
            where s.id = a.sessionId and a.tenantId = :tenantId and s.sessionDate between :from and :to
            group by s.classId, a.status
            """)
    List<Object[]> statsByClass(Long tenantId, LocalDate from, LocalDate to);

    /** [status, count] toàn trung tâm trong khoảng ngày. */
    @Query("""
            select a.status, count(a) from Attendance a, vn.educenter.classes.ClassSession s
            where s.id = a.sessionId and a.tenantId = :tenantId and s.sessionDate between :from and :to
            group by a.status
            """)
    List<Object[]> statsOverall(Long tenantId, LocalDate from, LocalDate to);

    /** Lịch sử điểm danh của 1 học viên: [attendance, session]. */
    @Query("""
            select a, s from Attendance a, vn.educenter.classes.ClassSession s
            where s.id = a.sessionId and a.tenantId = :tenantId and a.studentId = :studentId
            order by s.sessionDate desc, s.startTime desc
            """)
    List<Object[]> historyForStudent(Long tenantId, Long studentId);
}
