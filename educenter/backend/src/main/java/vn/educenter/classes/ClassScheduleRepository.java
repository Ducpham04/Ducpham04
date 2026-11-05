package vn.educenter.classes;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByClassIdOrderByDayOfWeekAscStartTimeAsc(Long classId);

    List<ClassSchedule> findByClassIdIn(Collection<Long> classIds);

    @Modifying
    @Query("delete from ClassSchedule s where s.classId = :classId")
    void deleteByClassId(Long classId);

    /** Khung giờ của các lớp khác còn hoạt động, cùng tenant, dùng chung phòng hoặc giáo viên. */
    @Query("""
            select new vn.educenter.classes.ConflictDetector$ExistingSlot(
                c.id, c.code, c.name, c.roomId, c.teacherId, c.startDate, c.endDate, s.dayOfWeek, s.startTime, s.endTime)
            from ClassSchedule s join ClassGroup c on c.id = s.classId
            where c.tenantId = :tenantId and c.id <> :excludeClassId
              and c.status in (vn.educenter.classes.ClassGroup.Status.PLANNED, vn.educenter.classes.ClassGroup.Status.ONGOING)
              and ((:roomId is not null and c.roomId = :roomId) or (:teacherId is not null and c.teacherId = :teacherId))
            """)
    List<ConflictDetector.ExistingSlot> findCandidateConflicts(Long tenantId, Long excludeClassId, Long roomId, Long teacherId);
}
