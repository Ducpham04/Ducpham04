package vn.bookeasy.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import vn.bookeasy.booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class CustomerDtos {
    private CustomerDtos() {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    }

    public record CustomerSummary(Long id, String fullName, String phone, String email, long totalBookings,
                                  long visits, long noShows, long cancelled, long totalSpent,
                                  LocalDateTime lastVisitAt, LocalDateTime createdAt) {
    }

    public record CustomerBooking(Long id, String code, LocalDateTime startAt, BookingStatus status,
                                  String serviceName, String staffName, long price) {
    }

    public record CustomerDetail(CustomerSummary customer, String notes, List<CustomerBooking> bookings) {
    }

    public record UpdateCustomerRequest(@Size(max = 100) String fullName,
                                        @Email(message = "Email không hợp lệ") @Size(max = 150) String email,
                                        @Size(max = 2000) String notes) {
    }
}
