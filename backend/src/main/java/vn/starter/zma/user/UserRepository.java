package vn.starter.zma.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByZaloId(String zaloId);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRole(Role role);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:q is null or lower(u.name) like :q or u.phone like :q or lower(u.email) like :q)
            """)
    Page<User> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);
}
