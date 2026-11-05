package vn.educenter.student;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface StudentMapper {

    StudentDtos.GuardianResponse toGuardian(Guardian guardian);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void update(StudentDtos.StudentRequest req, @MappingTarget Student student);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateGuardian(StudentDtos.GuardianRequest req, @MappingTarget Guardian guardian);

    default StudentDtos.StudentResponse toResponse(Student s, List<Guardian> guardians, String portalEmail) {
        return new StudentDtos.StudentResponse(s.getId(), s.getCode(), s.getFullName(), s.getDob(), s.getGender(),
                s.getPhone(), s.getEmail(), s.getSchool(), s.getLevel(), s.getAddress(), s.getStatus(), s.getJoinedAt(),
                s.getNote(), s.getCreatedAt(), guardians.stream().map(this::toGuardian).toList(), portalEmail);
    }
}
