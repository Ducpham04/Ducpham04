package vn.bookeasy.booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "booking_slots")
@Getter
@Setter
@NoArgsConstructor
public class BookingSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;
    @Column(nullable = false)
    private Long staffId;
    @Column(nullable = false)
    private LocalDateTime slotStart;

    public BookingSlot(Long bookingId, Long staffId, LocalDateTime slotStart) {
        this.bookingId = bookingId;
        this.staffId = staffId;
        this.slotStart = slotStart;
    }
}
