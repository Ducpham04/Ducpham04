package vn.bookeasy.staff;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface StaffTimeOffRepository extends JpaRepository<StaffTimeOff, Long> {

    List<StaffTimeOff> findByStaffIdOrderByStartDateDesc(Long staffId);

    @Query("select t from StaffTimeOff t where t.staffId in :ids and t.startDate <= :to and t.endDate >= :from")
    List<StaffTimeOff> findOverlapping(@Param("ids") Collection<Long> staffIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<StaffTimeOff> findByIdAndStaffId(Long id, Long staffId);
}
