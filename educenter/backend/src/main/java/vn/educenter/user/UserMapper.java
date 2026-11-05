package vn.educenter.user;

import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserDtos.UserResponse toResponse(User user);

    UserDtos.TeacherOption toOption(User user);
}
