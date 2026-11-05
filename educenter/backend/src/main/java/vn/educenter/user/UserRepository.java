package vn.educenter.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.educenter.security.Role;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findByTenantIdAndRoleInOrderByFullNameAsc(Long tenantId, Collection<Role> roles);

    List<User> findByTenantIdAndRoleAndEnabledTrueOrderByFullNameAsc(Long tenantId, Role role);

    Optional<User> findByTenantIdAndStudentId(Long tenantId, Long studentId);

    List<User> findByIdIn(Collection<Long> ids);

    long countByTenantId(Long tenantId);

    Optional<User> findFirstByTenantIdAndRoleOrderByIdAsc(Long tenantId, Role role);
}
