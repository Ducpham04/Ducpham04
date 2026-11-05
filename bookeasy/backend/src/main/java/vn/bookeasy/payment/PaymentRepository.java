package vn.bookeasy.payment;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByProviderAndProviderTxnId(String provider, String providerTxnId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
