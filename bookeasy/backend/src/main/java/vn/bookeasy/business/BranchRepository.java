package vn.bookeasy.business;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByBusinessIdOrderByIdAsc(Long businessId);

    List<Branch> findByBusinessIdAndActiveTrueOrderByIdAsc(Long businessId);

    Optional<Branch> findByIdAndBusinessId(Long id, Long businessId);
}
