package vn.starter.zma.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("select o from Order o where (:status is null or o.status = :status)")
    Page<Order> search(@Param("status") OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.code = :code")
    Optional<Order> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Query("select o.id from Order o where o.status = vn.starter.zma.order.OrderStatus.PENDING and o.createdAt < :before")
    List<Long> findStalePendingIds(@Param("before") Instant before);

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from Order o where o.status = vn.starter.zma.order.OrderStatus.PAID")
    long sumPaidAmount();
}
