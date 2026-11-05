package vn.educenter.classes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.attendance.AttendanceRepository;
import vn.educenter.catalog.CatalogService;
import vn.educenter.common.BusinessException;
import vn.educenter.common.ConflictException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.Role;
import vn.educenter.user.User;
import vn.educenter.user.UserService;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final ClassSessionRepository sessionRepository;
    private final ClassGroupRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final CatalogService catalogService;
    private final UserService userService;
    private final Lookups lookups;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ClassDtos.SessionView> range(LocalDate from, LocalDate to, Long teacherId, Long roomId, Long classId) {
        if (to.isBefore(from)) throw new BusinessException("Khoảng ngày không hợp lệ");
        if (ChronoUnit.DAYS.between(from, to) > 62) throw new BusinessException("Chỉ xem tối đa 62 ngày một lần");
        Long tenantId = CurrentUser.tenantId();
        List<ClassSession> sessions = sessionRepository.findRange(tenantId, from, to,
                ClassAccess.effectiveTeacherFilter(teacherId), roomId, classId);
        return toViews(tenantId, sessions);
    }

    @Transactional(readOnly = true)
    public List<ClassDtos.SessionView> today() {
        LocalDate today = LocalDate.now(clock);
        return range(today, today, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ClassDtos.SessionView> ofClass(Long classId) {
        ClassGroup c = classRepository.findByIdAndTenantId(classId, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("lớp học"));
        ClassAccess.ensureCanView(c);
        return toViews(c.getTenantId(), sessionRepository.findByTenantIdAndClassIdOrderBySessionDateAscStartTimeAsc(c.getTenantId(), classId));
    }

    @Transactional(readOnly = true)
    public ClassDtos.SessionView get(Long id) {
        ClassSession s = find(id);
        ClassGroup c = classRepository.findById(s.getClassId()).orElseThrow();
        ClassAccess.ensureCanTeach(s, c);
        return toViews(s.getTenantId(), List.of(s)).getFirst();
    }

    @Transactional
    public ClassDtos.SessionView update(Long id, ClassDtos.SessionUpdateRequest req) {
        ClassSession s = find(id);
        if (s.getStatus() == ClassSession.Status.COMPLETED && req.status() != ClassSession.Status.COMPLETED && req.status() != null) {
            throw new BusinessException("Buổi học đã điểm danh, không thể đổi trạng thái");
        }
        if (req.date() != null) s.setSessionDate(req.date());
        if (req.startTime() != null) s.setStartTime(req.startTime().withSecond(0).withNano(0));
        if (req.endTime() != null) s.setEndTime(req.endTime().withSecond(0).withNano(0));
        if (!s.getEndTime().isAfter(s.getStartTime())) throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu");
        if (req.roomId() != null) s.setRoomId(catalogService.findRoom(req.roomId()).getId());
        if (req.teacherId() != null) {
            User t = userService.findInTenant(req.teacherId());
            if (t.getRole() != Role.TEACHER) throw new BusinessException("Người được chọn không phải giáo viên");
            s.setTeacherId(t.getId());
        }
        if (req.status() != null) s.setStatus(req.status());
        if (req.note() != null) s.setNote(req.note().isBlank() ? null : req.note().trim());
        if (s.getStatus() != ClassSession.Status.CANCELLED) {
            List<ClassSession> overlaps = sessionRepository.findOverlapping(s.getTenantId(), s.getSessionDate(), s.getStartTime(),
                    s.getEndTime(), s.getRoomId(), s.getTeacherId(), s.getId());
            if (!overlaps.isEmpty()) {
                Map<Long, ClassGroup> classes = lookups.classes(s.getTenantId(), overlaps.stream().map(ClassSession::getClassId).toList());
                List<Map<String, Object>> details = overlaps.stream().map(o -> {
                    String type = Objects.equals(o.getRoomId(), s.getRoomId()) ? "ROOM" : "TEACHER";
                    ClassGroup oc = classes.get(o.getClassId());
                    return Map.<String, Object>of("type", type, "classId", o.getClassId(), "classCode", oc.getCode(),
                            "startTime", o.getStartTime().toString(), "endTime", o.getEndTime().toString());
                }).toList();
                ClassSession first = overlaps.getFirst();
                String what = Objects.equals(first.getRoomId(), s.getRoomId()) ? "Trùng phòng" : "Trùng giáo viên";
                throw new ConflictException(what + " với lớp " + classes.get(first.getClassId()).getCode() + " ("
                        + first.getStartTime() + "–" + first.getEndTime() + ")", details);
            }
        }
        return toViews(s.getTenantId(), List.of(s)).getFirst();
    }

    public ClassSession find(Long id) {
        return sessionRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("buổi học"));
    }

    /** Dùng cho cổng phụ huynh (đã tự kiểm tra quyền). */
    public List<ClassDtos.SessionView> toViewsPublic(Long tenantId, List<ClassSession> sessions) {
        return toViews(tenantId, sessions);
    }

    List<ClassDtos.SessionView> toViews(Long tenantId, List<ClassSession> sessions) {
        if (sessions.isEmpty()) return List.of();
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, sessions.stream().map(ClassSession::getClassId).toList());
        Map<Long, String> rooms = lookups.roomNames(tenantId, sessions.stream().map(ClassSession::getRoomId).toList());
        Map<Long, String> teachers = lookups.userNames(tenantId, sessions.stream().map(ClassSession::getTeacherId).toList());
        Map<Long, Long> attCounts = new HashMap<>();
        attendanceRepository.countBySession(tenantId, sessions.stream().map(ClassSession::getId).toList())
                .forEach(r -> attCounts.put((Long) r[0], (Long) r[1]));
        Map<Long, Long> studentCounts = new HashMap<>();
        enrollmentRepository.countActiveByClass(tenantId, classes.keySet()).forEach(r -> studentCounts.put((Long) r[0], (Long) r[1]));
        return sessions.stream().map(s -> {
            ClassGroup c = classes.get(s.getClassId());
            return new ClassDtos.SessionView(s.getId(), s.getClassId(), c.getCode(), c.getName(), s.getSessionDate(),
                    s.getStartTime(), s.getEndTime(), s.getRoomId(), rooms.get(s.getRoomId()), s.getTeacherId(),
                    teachers.get(s.getTeacherId()), s.getStatus(), s.getNote(), attCounts.getOrDefault(s.getId(), 0L),
                    studentCounts.getOrDefault(s.getClassId(), 0L));
        }).collect(Collectors.toList());
    }
}
