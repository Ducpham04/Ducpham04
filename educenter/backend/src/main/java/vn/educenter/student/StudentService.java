package vn.educenter.student;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.common.BusinessException;
import vn.educenter.common.CodeSequenceService;
import vn.educenter.common.NotFoundException;
import vn.educenter.common.PageResponse;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.Role;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;
import vn.educenter.user.UserService;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CodeSequenceService codeSequenceService;
    private final StudentMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** Phát ra khi học viên chuyển sang Đã nghỉ, để module lớp kết thúc các ghi danh. */
    public record StudentDroppedEvent(Long tenantId, Long studentId, LocalDate date) {}

    @Transactional(readOnly = true)
    public PageResponse<StudentDtos.StudentListItem> search(String q, Student.Status status, int page, int size) {
        Long tenantId = CurrentUser.tenantId();
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<Student> result = studentRepository.search(tenantId, query, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id")));
        List<Long> ids = result.getContent().stream().map(Student::getId).toList();
        Map<Long, Guardian> primary = ids.isEmpty() ? Map.of()
                : guardianRepository.findByTenantIdAndStudentIdIn(tenantId, ids).stream()
                .sorted((a, b) -> Boolean.compare(b.isPrimary(), a.isPrimary()))
                .collect(Collectors.toMap(Guardian::getStudentId, Function.identity(), (a, b) -> a));
        return PageResponse.of(result, s -> {
            Guardian g = primary.get(s.getId());
            return new StudentDtos.StudentListItem(s.getId(), s.getCode(), s.getFullName(), s.getPhone(), s.getStatus(),
                    s.getJoinedAt(), g == null ? null : g.getFullName(), g == null ? null : g.getPhone());
        });
    }

    @Transactional(readOnly = true)
    public StudentDtos.StudentResponse get(Long id) {
        Student s = find(id);
        return toResponse(s);
    }

    @Transactional
    public StudentDtos.StudentResponse create(StudentDtos.StudentRequest req) {
        Long tenantId = CurrentUser.tenantId();
        Student s = new Student();
        s.setTenantId(tenantId);
        mapper.update(req, s);
        normalize(s);
        s.setCode(codeSequenceService.nextCode(tenantId, "STUDENT", "HV", 4));
        s.setStatus(req.status() == null ? Student.Status.ACTIVE : req.status());
        s.setJoinedAt(req.joinedAt() == null ? LocalDate.now(clock) : req.joinedAt());
        studentRepository.save(s);
        return toResponse(s);
    }

    @Transactional
    public StudentDtos.StudentResponse update(Long id, StudentDtos.StudentRequest req) {
        Student s = find(id);
        mapper.update(req, s);
        normalize(s);
        if (req.joinedAt() != null) s.setJoinedAt(req.joinedAt());
        if (req.status() != null && req.status() != s.getStatus()) changeStatus(s, req.status());
        return toResponse(s);
    }

    @Transactional
    public StudentDtos.StudentResponse updateStatus(Long id, Student.Status status) {
        Student s = find(id);
        changeStatus(s, status);
        return toResponse(s);
    }

    private void changeStatus(Student s, Student.Status status) {
        s.setStatus(status);
        if (status == Student.Status.DROPPED) {
            events.publishEvent(new StudentDroppedEvent(s.getTenantId(), s.getId(), LocalDate.now(clock)));
        }
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.GuardianResponse> guardians(Long studentId) {
        find(studentId);
        return guardianRepository.findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(CurrentUser.tenantId(), studentId)
                .stream().map(mapper::toGuardian).toList();
    }

    @Transactional
    public StudentDtos.GuardianResponse addGuardian(Long studentId, StudentDtos.GuardianRequest req) {
        Student s = find(studentId);
        List<Guardian> existing = guardianRepository.findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(s.getTenantId(), studentId);
        Guardian g = new Guardian();
        g.setTenantId(s.getTenantId());
        g.setStudentId(studentId);
        mapper.updateGuardian(req, g);
        if (existing.isEmpty()) g.setPrimary(true);
        if (g.isPrimary()) existing.forEach(o -> o.setPrimary(false));
        return mapper.toGuardian(guardianRepository.save(g));
    }

    @Transactional
    public StudentDtos.GuardianResponse updateGuardian(Long guardianId, StudentDtos.GuardianRequest req) {
        Guardian g = guardianRepository.findByIdAndTenantId(guardianId, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("phụ huynh"));
        mapper.updateGuardian(req, g);
        if (g.isPrimary()) {
            guardianRepository.findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(g.getTenantId(), g.getStudentId())
                    .stream().filter(o -> !o.getId().equals(g.getId())).forEach(o -> o.setPrimary(false));
        }
        return mapper.toGuardian(g);
    }

    @Transactional
    public void deleteGuardian(Long guardianId) {
        Guardian g = guardianRepository.findByIdAndTenantId(guardianId, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("phụ huynh"));
        guardianRepository.delete(g);
        if (g.isPrimary()) {
            guardianRepository.findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(g.getTenantId(), g.getStudentId())
                    .stream().filter(o -> !o.getId().equals(g.getId())).findFirst().ifPresent(o -> o.setPrimary(true));
        }
    }

    /** Tạo hoặc đặt lại tài khoản cổng phụ huynh/học viên cho 1 học viên. */
    @Transactional
    public StudentDtos.StudentResponse upsertPortalAccount(Long studentId, StudentDtos.PortalAccountRequest req) {
        Student s = find(studentId);
        User existing = userRepository.findByTenantIdAndStudentId(s.getTenantId(), s.getId()).orElse(null);
        if (existing != null) {
            userRepository.delete(existing);
            userRepository.flush();
        }
        User u = userService.newUser(s.getTenantId(), req.email(), req.password(), s.getFullName(), Role.STUDENT);
        u.setStudentId(s.getId());
        userRepository.save(u);
        return toResponse(s);
    }

    public Student find(Long id) {
        return studentRepository.findByIdAndTenantId(id, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("học viên"));
    }

    private StudentDtos.StudentResponse toResponse(Student s) {
        List<Guardian> guardians = guardianRepository.findByTenantIdAndStudentIdOrderByPrimaryDescIdAsc(s.getTenantId(), s.getId());
        String portalEmail = userRepository.findByTenantIdAndStudentId(s.getTenantId(), s.getId()).map(User::getEmail).orElse(null);
        return mapper.toResponse(s, guardians, portalEmail);
    }

    private static void normalize(Student s) {
        s.setFullName(s.getFullName().trim().replaceAll("\\s+", " "));
        if (s.getPhone() != null && s.getPhone().isBlank()) s.setPhone(null);
        if (s.getEmail() != null && s.getEmail().isBlank()) s.setEmail(null);
        if (s.getDob() != null && s.getDob().getYear() < 1920) throw new BusinessException("Ngày sinh không hợp lệ");
    }
}
