package vn.starter.zma.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
            select p from Product p
            where (:activeOnly = false or p.active = true)
              and (:featured is null or p.featured = :featured)
              and (:q is null or lower(p.name) like :q)
            """)
    Page<Product> search(@Param("q") String q, @Param("featured") Boolean featured,
                         @Param("activeOnly") boolean activeOnly, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids")
    List<Product> findAllForUpdate(@Param("ids") Collection<Long> ids);
}
