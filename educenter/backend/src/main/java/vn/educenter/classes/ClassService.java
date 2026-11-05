package vn.educenter.classes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.attendance.Attendance;
import vn.educenter.attendance.AttendanceRepository;
import vn.educenter.catalog.CatalogService;
import vn.educenter.catalog.Course;
import vn.educenter.common.BusinessException;
import vn.educenter.common.ConflictException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.Role;
import vn.educenter.student.Student;
import vn.educenter.student.StudentService;
import vn.educenter.user.User;
import vn.educenter.user.UserService;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassGroupRepository classRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final CatalogService catalogService;
    private final StudentService studentService;
    private final UserService userService;
    private final Lookups lookups;
    private final Clock clock;

    // ---------- Đọc ----------

    @Transactional(readOnly = true)
    public List<ClassDtos.ClassListItem> list(String q, ClassGroup.Status status, Long teacherId) {
        Long tenantId = CurrentUser.tenantId();
        String query = q == null || q.isBlank() ? null : q.trim();
        List<ClassGroup> classes = classRepository.search(tenantId, query, status, ClassAccess.effectiveTeacherFilter(teacherId));
        return toListItems(tenantId, classes);
    }

    /** Dùng cho cổng phụ huynh (đã tự kiểm tra quyền). */
    public List<ClassDtos.ClassListItem> listForPortal(Long tenantId, List<Long> classIds) {
        return classIds.isEmpty() ? List.of() : toListItems(tenantId, classRepository.findByTenantIdAndIdIn(tenantId, classIds));
    }

    List<ClassDtos.ClassListItem> toListItems(Long tenantId, List<ClassGroup> classes) {
        if (classes.isEmpty()) return List.of();
        List<Long> ids = classes.stream().map(ClassGroup::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        enrollmentRepository.countActiveByClass(tenantId, ids).forEach(r -> counts.put((Long) r[0], (Long) r[1]));
        Map<Long, List<ClassDtos.SlotDto>> slots = scheduleRepository.findByClassIdIn(ids).stream()
                .sorted(Comparator.comparingInt(ClassSchedule::getDayOfWeek).thenComparing(ClassSchedule::getStartTime))
                .collect(Collectors.groupingBy(ClassSchedule::getClassId,
                        Collectors.mapping(s -> new ClassDtos.SlotDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()), Collectors.toList())));
        Map<Long, String> teachers = lookups.userNames(tenantId, classes.stream().map(ClassGroup::getTeacherId).toList());
        Map<Long, String> rooms = lookups.roomNames(tenantId, classes.stream().map(ClassGroup::getRoomId).toList());
        Map<Long, String> courses = lookups.courseNames(tenantId, classes.stream().map(ClassGroup::getCourseId).toList());
        return classes.stream().map(c -> new ClassDtos.ClassListItem(c.getId(), c.getCode(), c.getName(), c.getCourseId(),
                courses.get(c.getCourseId()), c.getTeacherId(), teachers.get(c.getTeacherId()), c.getRoomId(),
                rooms.get(c.getRoomId()), c.getMaxStudents(), counts.getOrDefault(c.getId(), 0L), c.getStartDate(),
                c.getEndDate(), c.getStatus(), slots.getOrDefault(c.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public ClassDtos.ClassDetail get(Long id) {
        ClassGroup c = find(id);
        ClassAccess.ensureCanView(c);
        return toDetail(c);
    }

    @Transactional(readOnly = true)
    public List<ClassDtos.ClassStudent> students(Long classId) {
        ClassGroup c = find(classId);
        ClassAccess.ensureCanView(c);
        Long tenantId = c.getTenantId();
        List<Enrollment> enrollments = enrollmentRepository.findByTenantIdAndClassIdOrderByStatusAscIdAsc(tenantId, classId);
        Map<Long, Student> students = lookups.students(tenantId, enrollments.stream().map(Enrollment::getStudentId).toList());
        Map<Long, long[]> stats = new HashMap<>();
        for (Object[] r : attendanceRepository.statsByStudentInClass(tenantId, classId)) {
            long[] s = stats.computeIfAbsent((Long) r[0], k -> new long[2]);
            long n = (Long) r[2];
            s[1] += n;
            if (((Attendance.Status) r[1]).attended()) s[0] += n;
        }
        return enrollments.stream().map(e -> {
            Student s = students.get(e.getStudentId());
            long[] st = stats.get(e.getStudentId());
            Double rate = st == null || st[1] == 0 ? null : Math.round(st[0] * 1000.0 / st[1]) / 10.0;
            return new ClassDtos.ClassStudent(e.getId(), e.getStudentId(), s == null ? null : s.getCode(),
                    s == null ? null : s.getFullName(), s == null ? null : s.getPhone(), s == null ? null : s.getStatus(),
                    e.getStartDate(), e.getEndDate(), e.getStatus(), rate);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ClassDtos.StudentEnrollment> enrollmentsOfStudent(Long studentId) {
        Student s = studentService.find(studentId);
        Long tenantId = s.getTenantId();
        List<Enrollment> list = enrollmentRepository.findByTenantIdAndStudentIdOrderByStartDateDesc(tenantId, studentId);
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, list.stream().map(Enrollment::getClassId).toList());
        Map<Long, String> courses = lookups.courseNames(tenantId, classes.values().stream().map(ClassGroup::getCourseId).toList());
        Map<Long, String> teachers = lookups.userNames(tenantId, classes.values().stream().map(ClassGroup::getTeacherId).toList());
        return list.stream().map(e -> {
            ClassGroup c = classes.get(e.getClassId());
            return new ClassDtos.StudentEnrollment(e.getId(), e.getClassId(), c.getCode(), c.getName(),
                    courses.get(c.getCourseId()), teachers.get(c.getTeacherId()), e.getStartDate(), e.getEndDate(),
                    e.getStatus(), c.getStatus());
        }).toList();
    }

    // ---------- Ghi ----------

    @Transactional
    public ClassDtos.ClassDetail create(ClassDtos.ClassRequest req) {
        Long tenantId = CurrentUser.tenantId();
        if (classRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, req.code())) {
            throw new BusinessException("Mã lớp " + req.code() + " đã tồn tại");
        }
        ClassGroup c = new ClassGroup();
        c.setTenantId(tenantId);
        apply(c, req);
        c.setStatus(req.status() == null ? ClassGroup.Status.PLANNED : req.status());
        classRepository.save(c);
        return toDetail(c);
    }

    @Transactional
    public ClassDtos.ClassDetail update(Long id, ClassDtos.ClassRequest req) {
        ClassGroup c = find(id);
        if (classRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(c.getTenantId(), req.code(), id)) {
            throw new BusinessException("Mã lớp " + req.code() + " đã tồn tại");
        }
        if (req.maxStudents() < enrollmentRepository.countByTenantIdAndClassIdAndStatus(c.getTenantId(), id, Enrollment.Status.ACTIVE)) {
            throw new BusinessException("Sĩ số tối đa nhỏ hơn số học viên đang học trong lớp");
        }
        boolean planningChanged = !Objects.equals(c.getRoomId(), req.roomId()) || !Objects.equals(c.getTeacherId(), req.teacherId())
                || !c.getStartDate().equals(req.startDate()) || !Objects.equals(c.getEndDate(), req.endDate());
        apply(c, req);
        if (req.status() != null) c.setStatus(req.status());
        List<ConflictDetector.Slot> slots = currentSlots(c.getId());
        if (planningChanged && c.getStatus().isOpen() && !slots.isEmpty()) {
            assertNoConflicts(c, slots);
            regenerateSessions(c, slots);
        }
        if (!c.getStatus().isOpen()) cancelFutureSessions(c);
        return toDetail(c);
    }

    @Transactional
    public ClassDtos.ClassDetail replaceSchedules(Long id, ClassDtos.SchedulesRequest req) {
        ClassGroup c = find(id);
        if (!c.getStatus().isOpen()) throw new BusinessException("Lớp đã kết thúc hoặc đã hủy, không thể đổi lịch");
        List<ConflictDetector.Slot> slots = req.slots().stream().map(ClassDtos.SlotDto::toSlot).toList();
        List<String> errors = ConflictDetector.validateOwnSlots(slots);
        if (!errors.isEmpty()) throw new BusinessException(errors.getFirst());
        assertNoConflicts(c, slots);
        scheduleRepository.deleteByClassId(c.getId());
        for (ConflictDetector.Slot s : slots) {
            ClassSchedule cs = new ClassSchedule();
            cs.setTenantId(c.getTenantId());
            cs.setClassId(c.getId());
            cs.setDayOfWeek(s.dayOfWeek());
            cs.setStartTime(s.startTime());
            cs.setEndTime(s.endTime());
            scheduleRepository.save(cs);
        }
        regenerateSessions(c, slots);
        return toDetail(c);
    }

    @Transactional(readOnly = true)
    public List<ConflictDetector.Conflict> checkConflicts(ClassDtos.ConflictCheckRequest req) {
        Long tenantId = CurrentUser.tenantId();
        List<ConflictDetector.Slot> slots = req.slots().stream().map(ClassDtos.SlotDto::toSlot).toList();
        long exclude = req.classId() == null ? -1L : req.classId();
        var existing = scheduleRepository.findCandidateConflicts(tenantId, exclude, req.roomId(), req.teacherId());
        return ConflictDetector.detect(req.roomId(), req.teacherId(), req.startDate(), req.endDate(), slots, existing);
    }

    @Transactional
    public ClassDtos.ClassStudent enroll(Long classId, ClassDtos.EnrollRequest req) {
        ClassGroup c = find(classId);
        if (!c.getStatus().isOpen()) throw new BusinessException("Lớp đã kết thúc hoặc đã hủy, không thể ghi danh");
        Student s = studentService.find(req.studentId());
        if (s.getStatus() == Student.Status.DROPPED) {
            throw new BusinessException("Học viên " + s.getFullName() + " đã nghỉ học, hãy đổi trạng thái trước khi ghi danh");
        }
        Long tenantId = c.getTenantId();
        if (enrollmentRepository.existsByTenantIdAndClassIdAndStudentIdAndStatus(tenantId, classId, s.getId(), Enrollment.Status.ACTIVE)) {
            throw new BusinessException("Học viên đã có trong lớp");
        }
        long active = enrollmentRepository.countByTenantIdAndClassIdAndStatus(tenantId, classId, Enrollment.Status.ACTIVE);
        if (active >= c.getMaxStudents()) {
            throw new BusinessException("Lớp đã đủ sĩ số (" + c.getMaxStudents() + " học viên)");
        }
        Enrollment e = new Enrollment();
        e.setTenantId(tenantId);
        e.setClassId(classId);
        e.setStudentId(s.getId());
        e.setStartDate(req.startDate() == null ? LocalDate.now(clock) : req.startDate());
        e.setStatus(Enrollment.Status.ACTIVE);
        enrollmentRepository.save(e);
        if (s.getStatus() == Student.Status.TRIAL || s.getStatus() == Student.Status.ON_HOLD) s.setStatus(Student.Status.ACTIVE);
        return new ClassDtos.ClassStudent(e.getId(), s.getId(), s.getCode(), s.getFullName(), s.getPhone(), s.getStatus(),
                e.getStartDate(), null, e.getStatus(), null);
    }

    @Transactional
    public void endEnrollment(Long enrollmentId, Enrollment.Status status) {
        Enrollment e = enrollmentRepository.findByIdAndTenantId(enrollmentId, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("ghi danh"));
        if (e.getStatus() != Enrollment.Status.ACTIVE) throw new BusinessException("Ghi danh này đã kết thúc");
        e.setStatus(status == null || status == Enrollment.Status.ACTIVE ? Enrollment.Status.DROPPED : status);
        e.setEndDate(LocalDate.now(clock));
    }

    @EventListener
    public void onStudentDropped(StudentService.StudentDroppedEvent event) {
        enrollmentRepository.findByTenantIdAndStudentIdAndStatus(event.tenantId(), event.studentId(), Enrollment.Status.ACTIVE)
                .forEach(e -> {
                    e.setStatus(Enrollment.Status.DROPPED);
                    e.setEndDate(event.date());
                });
    }

    // ---------- Nội bộ ----------

    public ClassGroup find(Long id) {
        return classRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("lớp học"));
    }

    private void apply(ClassGroup c, ClassDtos.ClassRequest req) {
        Course course = catalogService.findCourse(req.courseId());
        if (req.teacherId() != null) {
            User t = userService.findInTenant(req.teacherId());
            if (t.getRole() != Role.TEACHER) throw new BusinessException("Người được chọn không phải giáo viên");
        }
        if (req.roomId() != null) {
            var room = catalogService.findRoom(req.roomId());
            if (req.maxStudents() > room.getCapacity()) {
                throw new BusinessException("Sĩ số tối đa (" + req.maxStudents() + ") vượt sức chứa phòng " + room.getName() + " (" + room.getCapacity() + ")");
            }
        }
        c.setCourseId(course.getId());
        c.setCode(req.code().trim().toUpperCase());
        c.setName(req.name().trim());
        c.setTeacherId(req.teacherId());
        c.setRoomId(req.roomId());
        c.setMaxStudents(req.maxStudents());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        c.setNote(req.note());
    }

    private List<ConflictDetector.Slot> currentSlots(Long classId) {
        return scheduleRepository.findByClassIdOrderByDayOfWeekAscStartTimeAsc(classId).stream()
                .map(s -> new ConflictDetector.Slot(s.getDayOfWeek(), s.getStartTime(), s.getEndTime())).toList();
    }

    private void assertNoConflicts(ClassGroup c, List<ConflictDetector.Slot> slots) {
        var existing = scheduleRepository.findCandidateConflicts(c.getTenantId(), c.getId(), c.getRoomId(), c.getTeacherId());
        var conflicts = ConflictDetector.detect(c.getRoomId(), c.getTeacherId(), c.getStartDate(), c.getEndDate(), slots, existing);
        if (!conflicts.isEmpty()) {
            throw new ConflictException(conflicts.getFirst().message() + ". Chọn phòng/giáo viên khác hoặc đổi giờ.", conflicts);
        }
    }

    /** Xóa các buổi tương lai chưa điểm danh rồi sinh lại theo lịch mới. Buổi đã điểm danh được giữ nguyên. */
    private void regenerateSessions(ClassGroup c, List<ConflictDetector.Slot> slots) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = c.getStartDate().isAfter(today) ? c.getStartDate() : today;
        List<ClassSession> future = sessionRepository.findByTenantIdAndClassIdAndSessionDateGreaterThanEqual(c.getTenantId(), c.getId(), from);
        Set<Long> withAttendance = future.isEmpty() ? Set.of()
                : new HashSet<>(attendanceRepository.sessionIdsWithAttendance(future.stream().map(ClassSession::getId).toList()));
        List<Long> toDelete = new ArrayList<>();
        Set<String> keep = new HashSet<>();
        for (ClassSession s : future) {
            if (withAttendance.contains(s.getId()) || s.getStatus() == ClassSession.Status.COMPLETED) {
                keep.add(SessionGenerator.key(s.getSessionDate(), s.getStartTime()));
            } else {
                toDelete.add(s.getId());
            }
        }
        if (!toDelete.isEmpty()) sessionRepository.deleteByIds(toDelete);
        List<ClassSession> created = SessionGenerator.generate(from, c.getEndDate(), slots, keep).stream().map(o -> {
            ClassSession s = new ClassSession();
            s.setTenantId(c.getTenantId());
            s.setClassId(c.getId());
            s.setSessionDate(o.date());
            s.setStartTime(o.startTime());
            s.setEndTime(o.endTime());
            s.setRoomId(c.getRoomId());
            s.setTeacherId(c.getTeacherId());
            s.setStatus(ClassSession.Status.SCHEDULED);
            return s;
        }).toList();
        sessionRepository.saveAll(created);
    }

    private void cancelFutureSessions(ClassGroup c) {
        sessionRepository.findByTenantIdAndClassIdAndSessionDateGreaterThanEqual(c.getTenantId(), c.getId(), LocalDate.now(clock))
                .stream().filter(s -> s.getStatus() == ClassSession.Status.SCHEDULED)
                .forEach(s -> s.setStatus(ClassSession.Status.CANCELLED));
    }

    private ClassDtos.ClassDetail toDetail(ClassGroup c) {
        Long tenantId = c.getTenantId();
        ClassDtos.ClassListItem item = toListItems(tenantId, List.of(c)).getFirst();
        List<ClassSession> sessions = sessionRepository.findByTenantIdAndClassIdOrderBySessionDateAscStartTimeAsc(tenantId, c.getId());
        long total = sessions.stream().filter(s -> s.getStatus() != ClassSession.Status.CANCELLED).count();
        long completed = sessions.stream().filter(s -> s.getStatus() == ClassSession.Status.COMPLETED).count();
        return new ClassDtos.ClassDetail(c.getId(), c.getCode(), c.getName(), c.getCourseId(), item.courseName(),
                c.getTeacherId(), item.teacherName(), c.getRoomId(), item.roomName(), c.getMaxStudents(), item.enrolledCount(),
                c.getStartDate(), c.getEndDate(), c.getStatus(), c.getNote(), item.schedules(), total, completed);
    }
}
