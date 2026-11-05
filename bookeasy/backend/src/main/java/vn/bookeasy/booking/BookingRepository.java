package vn.bookeasy.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    String FETCH = "select b from Booking b join fetch b.customer join fetch b.staff join fetch b.service join fetch b.branch ";

    @Query(FETCH + "where b.id = :id and b.businessId = :bid")
    Optional<Booking> findDetailed(@Param("id") Long id, @Param("bid") Long businessId);

    @Query(FETCH + "where b.id = :id")
    Optional<Booking> findByIdDetailed(@Param("id") Long id);

    @Query(FETCH + "where b.code = :code")
    Optional<Booking> findByCodeDetailed(@Param("code") String code);

    @Query(FETCH + "where b.businessId = :bid and b.startAt >= :from and b.startAt < :to "
            + "and (:staffId is null or b.staff.id = :staffId) "
            + "and (:branchId is null or b.branch.id = :branchId) "
            + "and (:status is null or b.status = :status) "
            + "and (:q is null or lower(b.customer.fullName) like :q or b.customer.phone like :q or b.code like :q) "
            + "order by b.startAt asc")
    List<Booking> search(@Param("bid") Long businessId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                         @Param("staffId") Long staffId, @Param("branchId") Long branchId,
                         @Param("status") BookingStatus status, @Param("q") String q);

    @Query(FETCH + "where b.customer.id = :customerId order by b.startAt desc")
    List<Booking> findByCustomer(@Param("customerId") Long customerId);

    @Query("select count(b) from Booking b where b.staff.id = :staffId and b.startAt >= :from and b.startAt < :to and b.status in :statuses")
    long countForStaff(@Param("staffId") Long staffId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                       @Param("statuses") Collection<BookingStatus> statuses);

    @Query(FETCH + "where b.status = :status and b.startAt > :from and b.startAt <= :to and b.reminder24hSentAt is null")
    List<Booking> dueFor24hReminder(@Param("status") BookingStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(FETCH + "where b.status = :status and b.startAt > :from and b.startAt <= :to and b.reminder2hSentAt is null")
    List<Booking> dueFor2hReminder(@Param("status") BookingStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b.id from Booking b where b.status = :status and b.paymentExpiresAt < :now")
    List<Long> findExpiredPending(@Param("status") BookingStatus status, @Param("now") LocalDateTime now);

    @Query("select b.status, count(b) from Booking b where b.businessId = :bid and b.startAt >= :from and b.startAt < :to group by b.status")
    List<Object[]> countByStatus(@Param("bid") Long businessId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b.startAt, b.price from Booking b where b.businessId = :bid and b.status = :status and b.startAt >= :from and b.startAt < :to")
    List<Object[]> revenueRows(@Param("bid") Long businessId, @Param("status") BookingStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b.service.id, b.service.name, count(b), sum(b.price) from Booking b where b.businessId = :bid and b.status = :status "
            + "and b.startAt >= :from and b.startAt < :to group by b.service.id, b.service.name order by count(b) desc")
    List<Object[]> topServices(@Param("bid") Long businessId, @Param("status") BookingStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b.staff.id, b.staff.fullName, count(b), sum(b.price) from Booking b where b.businessId = :bid and b.status = :status "
            + "and b.startAt >= :from and b.startAt < :to group by b.staff.id, b.staff.fullName order by sum(b.price) desc")
    List<Object[]> topStaff(@Param("bid") Long businessId, @Param("status") BookingStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b.customer.id, b.status, count(b), sum(b.price), max(b.startAt) from Booking b where b.customer.id in :ids group by b.customer.id, b.status")
    List<Object[]> customerStats(@Param("ids") Collection<Long> customerIds);

    long countByBusinessId(Long businessId);
}
