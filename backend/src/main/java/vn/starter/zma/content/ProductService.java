package vn.starter.zma.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.starter.zma.common.dto.PageResponse;
import vn.starter.zma.common.exception.AppException;
import vn.starter.zma.common.util.PageRequests;
import vn.starter.zma.content.ContentDtos.ProductDto;
import vn.starter.zma.content.ContentDtos.ProductRequest;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final ContentMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(String q, Boolean featured, boolean activeOnly, int page, int size) {
        return PageResponse.of(repo.search(PageRequests.likePattern(q), featured, activeOnly,
                PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "featured", "id"))), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getActive(Long id) {
        return mapper.toDto(repo.findByIdAndActiveTrue(id).orElseThrow(() -> AppException.notFound("sản phẩm")));
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public ProductDto create(ProductRequest req) {
        Product p = new Product();
        mapper.update(p, req);
        return mapper.toDto(repo.save(p));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest req) {
        Product p = find(id);
        mapper.update(p, req);
        return mapper.toDto(p);
    }

    /** Xoá mềm (ẩn) để không phá lịch sử đơn hàng. */
    @Transactional
    public void delete(Long id) {
        find(id).setActive(false);
    }

    private Product find(Long id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("sản phẩm"));
    }
}
