package vn.educenter.classes;

import vn.educenter.common.ForbiddenException;
import vn.educenter.security.AuthUser;
import vn.educenter.security.CurrentUser;

/** Quyền sở hữu: quản lý (ADMIN/STAFF) xem mọi lớp; giáo viên chỉ lớp/buổi mình dạy. */
public final class ClassAccess {
    private ClassAccess() {
    }

    public static void ensureCanView(ClassGroup c) {
        AuthUser u = CurrentUser.get();
        if (u.isManager()) return;
        if (u.isTeacher() && u.userId().equals(c.getTeacherId())) return;
        throw new ForbiddenException();
    }

    public static void ensureCanTeach(ClassSession s, ClassGroup c) {
        AuthUser u = CurrentUser.get();
        if (u.isManager()) return;
        if (u.isTeacher() && (u.userId().equals(s.getTeacherId()) || u.userId().equals(c.getTeacherId()))) return;
        throw new ForbiddenException();
    }

    /** Giáo viên luôn bị ép lọc theo chính mình. */
    public static Long effectiveTeacherFilter(Long requested) {
        AuthUser u = CurrentUser.get();
        return u.isTeacher() ? u.userId() : requested;
    }
}
