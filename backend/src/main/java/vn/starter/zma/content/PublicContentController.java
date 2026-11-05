package vn.starter.zma.content;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.content.ContentDtos.*;

import java.util.List;

@Tag(name = "Content")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicContentController {
    private final ArticleService articleService;
    private final ProductService productService;

    @GetMapping("/articles")
    public PageResponse<ArticleSummaryDto> articles(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return articleService.search(q, category, true, page, size);
    }

    @GetMapping("/articles/categories")
    public List<String> articleCategories() {
        return articleService.categories();
    }

    @GetMapping("/articles/{id}")
    public ArticleDto article(@PathVariable Long id) {
        return articleService.getPublished(id);
    }

    @GetMapping("/products")
    public PageResponse<ProductDto> products(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean featured,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return productService.search(q, featured, true, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return productService.getActive(id);
    }
}
