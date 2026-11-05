package vn.starter.zma.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.util.PageRequests;
import vn.starter.zma.common.util.SlugUtils;
import vn.starter.zma.content.ContentDtos.*;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository repo;
    private final ContentMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummaryDto> search(String q, String category, boolean publishedOnly, int page, int size) {
        Sort sort = publishedOnly ? Sort.by(Sort.Direction.DESC, "publishedAt", "id") : Sort.by(Sort.Direction.DESC, "id");
        String cat = category == null || category.isBlank() ? null : category;
        return PageResponse.of(repo.search(PageRequests.likePattern(q), cat, publishedOnly,
                PageRequests.of(page, size, sort)), mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ArticleDto getPublished(Long id) {
        return mapper.toDto(repo.findByIdAndPublishedTrue(id).orElseThrow(() -> AppException.notFound("tin tức")));
    }

    @Transactional(readOnly = true)
    public ArticleDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return repo.findPublishedCategories();
    }

    @Transactional
    public ArticleDto create(ArticleRequest req) {
        Article a = new Article();
        mapper.update(a, req);
        a.setSlug(uniqueSlug(req.title()));
        applyPublish(a, req.published());
        return mapper.toDto(repo.save(a));
    }

    @Transactional
    public ArticleDto update(Long id, ArticleRequest req) {
        Article a = find(id);
        boolean wasPublished = a.isPublished();
        mapper.update(a, req);
        a.setPublished(wasPublished);
        applyPublish(a, req.published());
        return mapper.toDto(a);
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(find(id));
    }

    private void applyPublish(Article a, boolean publish) {
        if (publish && (!a.isPublished() || a.getPublishedAt() == null)) a.setPublishedAt(Instant.now());
        a.setPublished(publish);
    }

    private String uniqueSlug(String title) {
        String base = SlugUtils.slugify(title);
        if (base.isEmpty()) base = "tin";
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) slug = base + "-" + i++;
        return slug;
    }

    private Article find(Long id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("tin tức"));
    }
}
