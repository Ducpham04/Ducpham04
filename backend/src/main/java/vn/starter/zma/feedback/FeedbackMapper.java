package vn.starter.zma.feedback;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.starter.zma.feedback.FeedbackDtos.FeedbackDto;
import vn.starter.zma.feedback.FeedbackDtos.FeedbackSummaryDto;

import java.util.List;

@Mapper
public interface FeedbackMapper {
    @Mapping(target = "userName", source = "user.name")
    FeedbackSummaryDto toSummary(Feedback f);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userPhone", source = "user.phone")
    @Mapping(target = "images", source = "images")
    FeedbackDto toDto(Feedback f);

    default List<String> imageUrls(List<FeedbackImage> images) {
        return images == null ? List.of() : images.stream().map(FeedbackImage::getUrl).toList();
    }
}
