package vn.bookeasy.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CustomerQueryRepository extends Repository<Customer, Long> {
    @Query("select c from Customer c where c.businessId = :bid and (:q is null or lower(c.fullName) like :q or c.phone like :q) order by c.createdAt desc")
    Page<Customer> search(@Param("bid") Long businessId, @Param("q") String q, Pageable pageable);
}
