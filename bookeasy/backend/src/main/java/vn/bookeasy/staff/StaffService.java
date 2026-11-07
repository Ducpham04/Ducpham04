package vn.bookeasy.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.auth.Role;
import vn.bookeasy.auth.User;
import vn.bookeasy.auth.UserRepository;
import vn.bookeasy.business.BusinessService;
import vn.bookeasy.catalog.ServiceItem;
import vn.bookeasy.catalog.ServiceItemRepository;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.staff.StaffDtos.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final StaffRepository staffRepo;
    private final StaffShiftRepository shifts;
    private final StaffTimeOffRepository timeOffs;
    private final ServiceItemRepository services;
    private final UserRepository users;
    private final BusinessService businessService;
    private final PasswordEncoder passwordEncoder;

    public Staff require(Long bid, Long id) {
        return staffRepo.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("nhân viên"));
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> list(Long bid) {
        Map<Long, List<Long>> svcByStaff = new HashMap<>();
        services.findAllWithStaff(bid).forEach(s -> s.getStaff()
                .forEach(st -> svcByStaff.computeIfAbsent(st.getId(), k -> new ArrayList<>()).add(s.getId())));
        return staffRepo.findByBusinessIdOrderByIdAsc(bid).stream()
                .map(st -> toDto(st, svcByStaff.getOrDefault(st.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long bid, Long id) {
        Staff st = require(bid, id);
        return toDto(st, services.findByStaffId(id).stream().map(ServiceItem::getId).sorted().toList());
    }

    @Transactional
    public StaffResponse create(Long bid, StaffRequest req) {
        Staff st = new Staff();
        st.setBusinessId(bid);
        apply(bid, st, req);
        staffRepo.save(st);
        assignServices(bid, st, req.serviceIds());
        return get(bid, st.getId());
    }

    @Transactional
    public StaffResponse update(Long bid, Long id, StaffRequest req) {
        Staff st = require(bid, id);
        apply(bid, st, req);
        if (req.serviceIds() != null) assignServices(bid, st, req.serviceIds());
        users.findByStaffId(id).ifPresent(u -> u.setActive(st.isActive()));
        return get(bid, id);
    }

    /** Xóa mềm để giữ lịch sử booking; vô hiệu hóa tài khoản đăng nhập. */
    @Transactional
    public void deactivate(Long bid, Long id) {
        Staff st = require(bid, id);
        st.setActive(false);
        users.findByStaffId(id).ifPresent(u -> u.setActive(false));
    }

    private void apply(Long bid, Staff st, StaffRequest req) {
        businessService.requireBranch(bid, req.branchId());
        st.setFullName(req.fullName().trim());
        st.setTitle(TextUtils.blankToNull(req.title()));
        st.setPhone(TextUtils.blankToNull(req.phone()));
        st.setEmail(TextUtils.blankToNull(req.email()));
        st.setAvatarUrl(TextUtils.blankToNull(req.avatarUrl()));
        if (req.color() != null) st.setColor(req.color().toLowerCase());
        st.setBranchId(req.branchId());
        if (req.active() != null) st.setActive(req.active());
    }

    private void assignServices(Long bid, Staff st, List<Long> serviceIds) {
        Set<Long> wanted = serviceIds == null ? Set.of() : new HashSet<>(serviceIds);
        for (ServiceItem s : services.findAllWithStaff(bid)) {
            if (wanted.contains(s.getId())) s.getStaff().add(st);
            else s.getStaff().remove(st);
        }
        for (Long sid : wanted) {
            services.findByIdAndBusinessId(sid, bid).orElseThrow(() -> ApiException.notFound("dịch vụ"));
        }
    }

    private StaffResponse toDto(Staff st, List<Long> serviceIds) {
        Optional<User> u = users.findByStaffId(st.getId());
        return new StaffResponse(st.getId(), st.getFullName(), st.getTitle(), st.getPhone(), st.getEmail(),
                st.getAvatarUrl(), st.getColor(), st.getBranchId(), st.isActive(), serviceIds,
                u.isPresent(), u.map(User::getEmail).orElse(null));
    }

    // ---------- Ca làm việc ----------

    @Transactional(readOnly = true)
    public List<ShiftDto> getShifts(Long bid, Long staffId) {
        require(bid, staffId);
        return shifts.findByStaffIdOrderByDayOfWeekAscStartTimeAsc(staffId).stream()
                .map(s -> new ShiftDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime())).toList();
    }

    @Transactional
    public List<ShiftDto> replaceShifts(Long bid, Long staffId, ShiftsRequest req) {
        require(bid, staffId);
        Map<Integer, List<ShiftDto>> byDay = new HashMap<>();
        for (ShiftDto s : req.shifts()) {
            if (!s.startTime().isBefore(s.endTime())) {
                throw ApiException.badRequest("INVALID_SHIFT", "Giờ bắt đầu ca phải trước giờ kết thúc");
            }
            byDay.computeIfAbsent(s.dayOfWeek(), k -> new ArrayList<>()).add(s);
        }
        for (List<ShiftDto> day : byDay.values()) {
            day.sort(Comparator.comparing(ShiftDto::startTime));
            for (int i = 1; i < day.size(); i++) {
                if (day.get(i).startTime().isBefore(day.get(i - 1).endTime())) {
                    throw ApiException.badRequest("INVALID_SHIFT", "Các ca trong cùng một ngày bị chồng lên nhau");
                }
            }
        }
        shifts.deleteByStaffId(staffId);
        shifts.flush();
        for (ShiftDto dto : req.shifts()) {
            StaffShift s = new StaffShift();
            s.setStaffId(staffId);
            s.setDayOfWeek(dto.dayOfWeek());
            s.setStartTime(dto.startTime());
            s.setEndTime(dto.endTime());
            shifts.save(s);
        }
        return getShifts(bid, staffId);
    }

    // ---------- Ngày nghỉ ----------

    @Transactional(readOnly = true)
    public List<TimeOffResponse> listTimeOff(Long bid, Long staffId) {
        require(bid, staffId);
        return timeOffs.findByStaffIdOrderByStartDateDesc(staffId).stream()
                .map(t -> new TimeOffResponse(t.getId(), t.getStartDate(), t.getEndDate(), t.getReason())).toList();
    }

    @Transactional
    public TimeOffResponse addTimeOff(Long bid, Long staffId, TimeOffRequest req) {
        require(bid, staffId);
        if (req.endDate().isBefore(req.startDate())) {
            throw ApiException.badRequest("INVALID_RANGE", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        StaffTimeOff t = new StaffTimeOff();
        t.setStaffId(staffId);
        t.setStartDate(req.startDate());
        t.setEndDate(req.endDate());
        t.setReason(TextUtils.blankToNull(req.reason()));
        timeOffs.save(t);
        return new TimeOffResponse(t.getId(), t.getStartDate(), t.getEndDate(), t.getReason());
    }

    @Transactional
    public void deleteTimeOff(Long bid, Long staffId, Long id) {
        require(bid, staffId);
        timeOffs.delete(timeOffs.findByIdAndStaffId(id, staffId).orElseThrow(() -> ApiException.notFound("ngày nghỉ")));
    }

    // ---------- Tài khoản ----------

    @Transactional
    public StaffResponse createAccount(Long bid, Long staffId, AccountRequest req) {
        Staff st = require(bid, staffId);
        if (users.findByStaffId(staffId).isPresent()) {
            throw ApiException.conflict("ACCOUNT_EXISTS", "Nhân viên này đã có tài khoản");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        User u = new User();
        u.setBusinessId(bid);
        u.setStaffId(staffId);
        u.setEmail(email);
        u.setFullName(st.getFullName());
        u.setRole(Role.STAFF);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(u);
        return get(bid, staffId);
    }
}
