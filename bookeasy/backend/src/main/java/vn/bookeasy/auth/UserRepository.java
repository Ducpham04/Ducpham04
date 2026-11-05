package vn.bookeasy.auth;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByStaffId(Long staffId);

    Optional<User> findFirstByBusinessIdAndRoleOrderByIdAsc(Long businessId, Role role);
}
