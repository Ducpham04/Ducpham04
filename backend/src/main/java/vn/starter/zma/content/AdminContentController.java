package vn.starter.zma.content;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.content.ContentDtos.*;

@Tag(name = "Admin - Content")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminContentController {
    private final ArticleService articleService;
    private final ProductService productService;

    @GetMapping("/articles")
    public PageResponse<ArticleSummaryDto> articles(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String category,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return articleService.search(q, category, false, page, size);
    }

    @GetMapping("/articles/{id}")
    public ArticleDto article(@PathVariable Long id) {
        return articleService.get(id);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDto createArticle(@Valid @RequestBody ArticleRequest req) {
        return articleService.create(req);
    }

    @PutMapping("/articles/{id}")
    public ArticleDto updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest req) {
        return articleService.update(id, req);
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products")
    public PageResponse<ProductDto> products(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return productService.search(q, null, false, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @PutMapping("/products/{id}")
    public ProductDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
