package vn.bookeasy.catalog;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    @Query("select distinct s from ServiceItem s left join fetch s.staff where s.businessId = :bid order by s.sortOrder, s.id")
    List<ServiceItem> findAllWithStaff(@Param("bid") Long businessId);

    @Query("select distinct s from ServiceItem s left join fetch s.staff where s.businessId = :bid and s.active = true order by s.sortOrder, s.id")
    List<ServiceItem> findActiveWithStaff(@Param("bid") Long businessId);

    Optional<ServiceItem> findByIdAndBusinessId(Long id, Long businessId);

    @Query("select s from ServiceItem s left join fetch s.staff where s.id = :id and s.businessId = :bid")
    Optional<ServiceItem> findWithStaff(@Param("id") Long id, @Param("bid") Long businessId);

    @Query("select s from ServiceItem s join s.staff st where st.id = :staffId")
    List<ServiceItem> findByStaffId(@Param("staffId") Long staffId);
}
