package vn.bookeasy.business;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface BranchHourRepository extends JpaRepository<BranchHour, Long> {

    List<BranchHour> findByBranchIdOrderByDayOfWeekAsc(Long branchId);

    List<BranchHour> findByBranchIdIn(Collection<Long> branchIds);

    Optional<BranchHour> findByBranchIdAndDayOfWeek(Long branchId, int dayOfWeek);

    @Modifying
    @Query("delete from BranchHour h where h.branchId = :branchId")
    void deleteByBranchId(@Param("branchId") Long branchId);
}
