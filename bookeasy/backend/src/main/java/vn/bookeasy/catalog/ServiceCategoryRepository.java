package vn.bookeasy.catalog;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByBusinessIdOrderBySortOrderAscIdAsc(Long businessId);

    Optional<ServiceCategory> findByIdAndBusinessId(Long id, Long businessId);
}
