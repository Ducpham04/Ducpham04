package vn.educenter.user;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.auth.RefreshTokenRepository;
import vn.educenter.common.BusinessException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.Role;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final EnumSet<Role> STAFF_ROLES = EnumSet.of(Role.CENTER_ADMIN, Role.STAFF, Role.TEACHER);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> listStaff() {
        return userRepository.findByTenantIdAndRoleInOrderByFullNameAsc(CurrentUser.tenantId(), STAFF_ROLES)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.TeacherOption> listTeachers() {
        return userRepository.findByTenantIdAndRoleAndEnabledTrueOrderByFullNameAsc(CurrentUser.tenantId(), Role.TEACHER)
                .stream().map(mapper::toOption).toList();
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest req) {
        ensureStaffRole(req.role());
        User user = newUser(CurrentUser.tenantId(), req.email(), req.password(), req.fullName(), req.role());
        user.setPhone(blankToNull(req.phone()));
        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest req) {
        ensureStaffRole(req.role());
        User user = findInTenant(id);
        if (user.getRole() == Role.STUDENT) throw new NotFoundException("Không tìm thấy nhân viên");
        if (user.getId().equals(CurrentUser.get().userId()) && req.role() != user.getRole()) {
            throw new BusinessException("Bạn không thể tự đổi vai trò của chính mình");
        }
        user.setFullName(req.fullName().trim());
        user.setPhone(blankToNull(req.phone()));
        user.setRole(req.role());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        return mapper.toResponse(user);
    }

    @Transactional
    public UserDtos.UserResponse setEnabled(Long id, boolean enabled) {
        User user = findInTenant(id);
        if (user.getId().equals(CurrentUser.get().userId())) {
            throw new BusinessException("Bạn không thể tự khóa tài khoản của mình");
        }
        user.setEnabled(enabled);
        if (!enabled) refreshTokenRepository.revokeAllForUser(user.getId());
        return mapper.toResponse(user);
    }

    /** Tạo user mới (dùng chung cho nhân viên, chủ trung tâm, tài khoản cổng). Kiểm tra trùng email toàn hệ thống. */
    public User newUser(Long tenantId, String email, String rawPassword, String fullName, Role role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new BusinessException("Email " + normalized + " đã được sử dụng");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    public User findInTenant(Long id) {
        return userRepository.findByIdAndTenantId(id, CurrentUser.tenantId())
                .orElseThrow(() -> NotFoundException.of("tài khoản"));
    }

    private static void ensureStaffRole(Role role) {
        if (!STAFF_ROLES.contains(role)) throw new BusinessException("Vai trò không hợp lệ cho nhân viên");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
