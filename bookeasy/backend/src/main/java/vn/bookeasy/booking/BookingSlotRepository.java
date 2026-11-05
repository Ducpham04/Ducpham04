package vn.bookeasy.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {

    @Modifying
    @Query("delete from BookingSlot s where s.bookingId = :bookingId")
    void deleteByBookingId(@Param("bookingId") Long bookingId);

    @Query("select s from BookingSlot s where s.staffId in :staffIds and s.slotStart >= :from and s.slotStart < :to")
    List<BookingSlot> findBusy(@Param("staffIds") Collection<Long> staffIds, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
