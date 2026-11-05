package vn.bookeasy.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.booking.SlotCalculator.Window;
import vn.bookeasy.business.*;
import vn.bookeasy.catalog.ServiceItem;
import vn.bookeasy.staff.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final BranchHourRepository branchHours;
    private final HolidayRepository holidays;
    private final StaffRepository staffRepo;
    private final StaffShiftRepository shifts;
    private final StaffTimeOffRepository timeOffs;
    private final BookingSlotRepository slots;
    private final Clock clock;

    public record SlotOption(LocalTime time, List<Long> staffIds) {
    }

    public record DayAvailability(LocalDate date, boolean available, int slotCount) {
    }

    /** Nhân viên ứng viên: đang hoạt động, thuộc chi nhánh, làm được dịch vụ (lọc theo staffId nếu có). */
    public List<Staff> candidates(ServiceItem service, Long branchId, Long staffId) {
        List<Staff> list = staffRepo.findActiveForService(service.getId(), branchId);
        if (staffId != null) list = list.stream().filter(s -> s.getId().equals(staffId)).toList();
        return list;
    }

    @Transactional(readOnly = true)
    public List<SlotOption> slotsForDay(Business business, Long branchId, ServiceItem service, Long staffId, LocalDate date) {
        return compute(business, branchId, service, candidates(service, branchId, staffId), date);
    }

    @Transactional(readOnly = true)
    public List<DayAvailability> days(Business business, Long branchId, ServiceItem service, Long staffId,
                                      LocalDate from, int count) {
        List<Staff> cands = candidates(service, branchId, staffId);
        LocalDate today = LocalDate.now(clock);
        LocalDate last = today.plusDays(business.getBookingWindowDays() - 1L);
        List<DayAvailability> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDate d = from.plusDays(i);
            if (d.isBefore(today) || d.isAfter(last)) {
                result.add(new DayAvailability(d, false, 0));
                continue;
            }
            int n = compute(business, branchId, service, cands, d).size();
            result.add(new DayAvailability(d, n > 0, n));
        }
        return result;
    }

    List<SlotOption> compute(Business business, Long branchId, ServiceItem service, List<Staff> cands, LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (cands.isEmpty() || date.isBefore(today) || date.isAfter(today.plusDays(business.getBookingWindowDays() - 1L))) {
            return List.of();
        }
        if (!holidays.findApplicable(business.getId(), branchId, date, date).isEmpty()) return List.of();
        int dow = date.getDayOfWeek().getValue();
        Optional<BranchHour> hours = branchHours.findByBranchIdAndDayOfWeek(branchId, dow);
        if (hours.isEmpty() || hours.get().isClosed()) return List.of();
        Window open = new Window(hours.get().getOpenTime(), hours.get().getCloseTime());

        List<Long> ids = cands.stream().map(Staff::getId).toList();
        Set<Long> off = timeOffs.findOverlapping(ids, date, date).stream().map(StaffTimeOff::getStaffId).collect(Collectors.toSet());
        Map<Long, List<StaffShift>> shiftsByStaff = shifts.findByStaffIdInAndDayOfWeek(ids, dow).stream()
                .collect(Collectors.groupingBy(StaffShift::getStaffId));
        Map<Long, Set<LocalDateTime>> busyByStaff = slots.findBusy(ids, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(BookingSlot::getStaffId,
                        Collectors.mapping(BookingSlot::getSlotStart, Collectors.toSet())));
        LocalDateTime earliest = LocalDateTime.now(clock).plusMinutes(business.getMinLeadMinutes());

        TreeMap<LocalTime, List<Long>> byTime = new TreeMap<>();
        for (Staff st : cands) {
            if (off.contains(st.getId())) continue;
            List<Window> windows = shiftsByStaff.getOrDefault(st.getId(), List.of()).stream()
                    .map(s -> new Window(s.getStartTime(), s.getEndTime()).intersect(open))
                    .filter(Objects::nonNull).toList();
            for (LocalTime t : SlotCalculator.availableStarts(date, windows,
                    busyByStaff.getOrDefault(st.getId(), Set.of()), service.getDurationMinutes(), earliest)) {
                byTime.computeIfAbsent(t, k -> new ArrayList<>()).add(st.getId());
            }
        }
        return byTime.entrySet().stream().map(e -> new SlotOption(e.getKey(), e.getValue())).toList();
    }
}
