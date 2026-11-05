package vn.educenter.tenant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface TenantMapper {

    @Mapping(target = "studentCount", ignore = true)
    @Mapping(target = "userCount", ignore = true)
    TenantDtos.TenantResponse toResponse(Tenant tenant);

    void updateCenter(TenantDtos.CenterRequest request, @MappingTarget Tenant tenant);
}
