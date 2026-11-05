package vn.bookeasy.staff;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByBusinessIdOrderByIdAsc(Long businessId);

    Optional<Staff> findByIdAndBusinessId(Long id, Long businessId);

    @Query("select st from ServiceItem s join s.staff st where s.id = :serviceId and st.active = true and st.branchId = :branchId order by st.id")
    List<Staff> findActiveForService(@Param("serviceId") Long serviceId, @Param("branchId") Long branchId);
}
