package vn.bookeasy.staff;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {

    List<StaffShift> findByStaffIdOrderByDayOfWeekAscStartTimeAsc(Long staffId);

    List<StaffShift> findByStaffIdInAndDayOfWeek(Collection<Long> staffIds, int dayOfWeek);

    @Modifying
    @Query("delete from StaffShift s where s.staffId = :staffId")
    void deleteByStaffId(@Param("staffId") Long staffId);
}
