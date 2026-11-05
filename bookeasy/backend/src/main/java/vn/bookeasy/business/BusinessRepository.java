package vn.bookeasy.business;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Business> findAllByOrderByCreatedAtDesc();
}
