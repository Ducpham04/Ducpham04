package vn.starter.zma.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<Feedback> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            select f from Feedback f
            where (:status is null or f.status = :status)
              and (:q is null or lower(f.title) like :q or lower(f.code) like :q)
            """)
    Page<Feedback> search(@Param("q") String q, @Param("status") FeedbackStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    @Query("select f from Feedback f where f.id = :id")
    Optional<Feedback> findDetailById(@Param("id") Long id);

    long countByStatus(FeedbackStatus status);

    @Query("select f.status, count(f) from Feedback f group by f.status")
    List<Object[]> countGroupByStatus();
}
