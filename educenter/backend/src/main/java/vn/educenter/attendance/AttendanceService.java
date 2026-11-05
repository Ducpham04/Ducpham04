package vn.educenter.attendance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.classes.*;
import vn.educenter.common.BusinessException;
import vn.educenter.security.CurrentUser;
import vn.educenter.student.Student;
import vn.educenter.student.StudentService;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final SessionService sessionService;
    private final ClassGroupRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final Lookups lookups;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AttendanceDtos.SessionAttendance forSession(Long sessionId) {
        ClassSession s = sessionService.find(sessionId);
        ClassGroup c = classRepository.findById(s.getClassId()).orElseThrow();
        ClassAccess.ensureCanTeach(s, c);
        Long tenantId = s.getTenantId();
        Map<Long, Attendance> existing = new HashMap<>();
        attendanceRepository.findByTenantIdAndSessionId(tenantId, sessionId).forEach(a -> existing.put(a.getStudentId(), a));
        Set<Long> studentIds = new LinkedHashSet<>();
        enrollmentRepository.findByTenantIdAndClassIdAndStatus(tenantId, c.getId(), Enrollment.Status.ACTIVE).stream()
                .filter(e -> !e.getStartDate().isAfter(s.getSessionDate()))
                .forEach(e -> studentIds.add(e.getStudentId()));
        studentIds.addAll(existing.keySet());
        Map<Long, Student> students = lookups.students(tenantId, studentIds);
        List<AttendanceDtos.RosterItem> items = studentIds.stream().map(id -> {
            Student st = students.get(id);
            Attendance a = existing.get(id);
            return new AttendanceDtos.RosterItem(id, st == null ? null : st.getCode(), st == null ? "?" : st.getFullName(),
                    a == null ? null : a.getStatus(), a == null ? null : a.getNote());
        }).sorted(Comparator.comparing(AttendanceDtos.RosterItem::studentName, Comparator.nullsLast(lastNameComparator()))).toList();
        return new AttendanceDtos.SessionAttendance(sessionService.get(sessionId), items, !existing.isEmpty());
    }

    @Transactional
    public AttendanceDtos.SessionAttendance save(Long sessionId, AttendanceDtos.SaveRequest req) {
        ClassSession s = sessionService.find(sessionId);
        ClassGroup c = classRepository.findById(s.getClassId()).orElseThrow();
        ClassAccess.ensureCanTeach(s, c);
        if (s.getStatus() == ClassSession.Status.CANCELLED) throw new BusinessException("Buổi học đã hủy, không thể điểm danh");
        if (s.getSessionDate().isAfter(LocalDate.now(clock))) throw new BusinessException("Chưa đến ngày học, chưa thể điểm danh");
        Long tenantId = s.getTenantId();
        Set<Long> allowed = new HashSet<>();
        enrollmentRepository.findByTenantIdAndClassIdOrderByStatusAscIdAsc(tenantId, c.getId()).forEach(e -> allowed.add(e.getStudentId()));
        Map<Long, Attendance> existing = new HashMap<>();
        attendanceRepository.findByTenantIdAndSessionId(tenantId, sessionId).forEach(a -> existing.put(a.getStudentId(), a));
        Long userId = CurrentUser.get().userId();
        var now = clock.instant();
        for (AttendanceDtos.MarkItem item : req.items()) {
            if (!allowed.contains(item.studentId())) throw new BusinessException("Có học viên không thuộc lớp này");
            Attendance a = existing.get(item.studentId());
            if (a == null) {
                a = new Attendance();
                a.setTenantId(tenantId);
                a.setSessionId(sessionId);
                a.setStudentId(item.studentId());
            }
            a.setStatus(item.status());
            a.setNote(item.note() == null || item.note().isBlank() ? null : item.note().trim());
            a.setMarkedBy(userId);
            a.setMarkedAt(now);
            attendanceRepository.save(a);
        }
        s.setStatus(ClassSession.Status.COMPLETED);
        if (c.getStatus() == ClassGroup.Status.PLANNED) c.setStatus(ClassGroup.Status.ONGOING);
        attendanceRepository.flush();
        return forSession(sessionId);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.ClassRate> classRates(LocalDate from, LocalDate to) {
        Long tenantId = CurrentUser.tenantId();
        Map<Long, long[]> agg = new LinkedHashMap<>();
        for (Object[] r : attendanceRepository.statsByClass(tenantId, from, to)) {
            long[] a = agg.computeIfAbsent((Long) r[0], k -> new long[4]);
            a[((Attendance.Status) r[1]).ordinal()] += (Long) r[2];
        }
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, agg.keySet());
        return agg.entrySet().stream().map(e -> {
            long[] a = e.getValue();
            long total = a[0] + a[1] + a[2] + a[3];
            ClassGroup c = classes.get(e.getKey());
            return new AttendanceDtos.ClassRate(e.getKey(), c.getCode(), c.getName(), a[0], a[1], a[2], a[3], total, rate(a[0] + a[1], total));
        }).sorted(Comparator.comparing(AttendanceDtos.ClassRate::classCode)).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.StudentAttendance studentHistory(Long studentId) {
        Student st = studentService.find(studentId);
        return historyOf(st.getTenantId(), st.getId());
    }

    public AttendanceDtos.StudentAttendance historyOf(Long tenantId, Long studentId) {
        List<Object[]> rows = attendanceRepository.historyForStudent(tenantId, studentId);
        Map<Long, ClassGroup> classes = lookups.classes(tenantId, rows.stream().map(r -> ((ClassSession) r[1]).getClassId()).toList());
        long attended = 0;
        List<AttendanceDtos.HistoryItem> items = new ArrayList<>();
        for (Object[] r : rows) {
            Attendance a = (Attendance) r[0];
            ClassSession s = (ClassSession) r[1];
            ClassGroup c = classes.get(s.getClassId());
            if (a.getStatus().attended()) attended++;
            items.add(new AttendanceDtos.HistoryItem(s.getId(), c.getCode(), c.getName(), s.getSessionDate(), s.getStartTime(), a.getStatus(), a.getNote()));
        }
        return new AttendanceDtos.StudentAttendance(rows.size(), attended, rate(attended, rows.size()), items);
    }

    public static double rate(long attended, long total) {
        return total == 0 ? 0 : Math.round(attended * 1000.0 / total) / 10.0;
    }

    /** Sắp theo tên (từ cuối) như sổ điểm danh Việt Nam. */
    static Comparator<String> lastNameComparator() {
        java.text.Collator collator = java.text.Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        return Comparator.comparing((String n) -> n.substring(n.lastIndexOf(' ') + 1), collator).thenComparing(collator);
    }
}
