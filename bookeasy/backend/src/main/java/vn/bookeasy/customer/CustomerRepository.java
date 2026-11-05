package vn.bookeasy.customer;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByBusinessIdAndPhone(Long businessId, String phone);

    Optional<Customer> findByIdAndBusinessId(Long id, Long businessId);
}
