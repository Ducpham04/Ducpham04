package vn.bookeasy.business;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByBusinessIdAndHolidayDateGreaterThanEqualOrderByHolidayDateAsc(Long businessId, LocalDate from);

    @Query("select h from Holiday h where h.businessId = :bid and h.holidayDate between :from and :to and (h.branchId is null or h.branchId = :branchId)")
    List<Holiday> findApplicable(@Param("bid") Long businessId, @Param("branchId") Long branchId,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<Holiday> findByIdAndBusinessId(Long id, Long businessId);
}
