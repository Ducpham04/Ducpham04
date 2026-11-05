package vn.bookeasy.business;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.bookeasy.business.BusinessDtos.*;

import java.util.List;

@Mapper
public interface BusinessMapper {
    BusinessResponse toResponse(Business business);

    BranchHourDto toDto(BranchHour hour);

    @Mapping(target = "hours", source = "hours")
    BranchResponse toResponse(Branch branch, List<BranchHourDto> hours);

    @Mapping(target = "date", source = "holidayDate")
    HolidayResponse toResponse(Holiday holiday);
}
