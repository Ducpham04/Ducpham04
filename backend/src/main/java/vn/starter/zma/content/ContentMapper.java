package vn.starter.zma.content;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.starter.zma.content.ContentDtos.*;

@Mapper
public interface ContentMapper {
    ArticleSummaryDto toSummary(Article a);

    ArticleDto toDto(Article a);

    ProductDto toDto(Product p);

    void update(@MappingTarget Article target, ArticleRequest req);

    void update(@MappingTarget Product target, ProductRequest req);
}
