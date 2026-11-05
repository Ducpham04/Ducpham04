package vn.starter.zma.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Query("""
            select a from Article a
            where (:publishedOnly = false or a.published = true)
              and (:category is null or a.category = :category)
              and (:q is null or lower(a.title) like :q)
            """)
    Page<Article> search(@Param("q") String q, @Param("category") String category,
                         @Param("publishedOnly") boolean publishedOnly, Pageable pageable);

    Optional<Article> findByIdAndPublishedTrue(Long id);

    boolean existsBySlug(String slug);

    @Query("select distinct a.category from Article a where a.published = true order by a.category")
    List<String> findPublishedCategories();
}
