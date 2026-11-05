package vn.educenter.classes;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.educenter.catalog.Course;
import vn.educenter.catalog.CourseRepository;
import vn.educenter.catalog.Room;
import vn.educenter.catalog.RoomRepository;
import vn.educenter.student.Student;
import vn.educenter.student.StudentRepository;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;

/** Tra cứu tên theo lô (tránh N+1) cho các màn danh sách. */
@Component
@RequiredArgsConstructor
public class Lookups {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classRepository;

    public Map<Long, String> userNames(Long tenantId, Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) return Map.of();
        return userRepository.findByIdIn(clean).stream().filter(u -> tenantId.equals(u.getTenantId()))
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    public Map<Long, String> roomNames(Long tenantId, Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) return Map.of();
        return roomRepository.findByTenantIdAndIdIn(tenantId, clean).stream().collect(Collectors.toMap(Room::getId, Room::getName));
    }

    public Map<Long, String> courseNames(Long tenantId, Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) return Map.of();
        return courseRepository.findByTenantIdAndIdIn(tenantId, clean).stream().collect(Collectors.toMap(Course::getId, Course::getName));
    }

    public Map<Long, Student> students(Long tenantId, Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) return Map.of();
        return studentRepository.findByTenantIdAndIdIn(tenantId, clean).stream().collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    public Map<Long, ClassGroup> classes(Long tenantId, Collection<Long> ids) {
        var clean = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (clean.isEmpty()) return Map.of();
        return classRepository.findByTenantIdAndIdIn(tenantId, clean).stream().collect(Collectors.toMap(ClassGroup::getId, Function.identity()));
    }
}
