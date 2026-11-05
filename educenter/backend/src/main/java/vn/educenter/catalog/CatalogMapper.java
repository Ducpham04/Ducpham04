package vn.educenter.catalog;

import org.mapstruct.Mapper;

@Mapper
public interface CatalogMapper {
    CatalogDtos.CourseResponse toResponse(Course course);

    CatalogDtos.RoomResponse toResponse(Room room);
}
