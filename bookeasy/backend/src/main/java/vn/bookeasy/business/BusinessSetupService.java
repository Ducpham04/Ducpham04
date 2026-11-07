package vn.bookeasy.business;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/** Khởi tạo cơ sở mới với chi nhánh và giờ mở cửa mặc định. */
@Service
@RequiredArgsConstructor
public class BusinessSetupService {
    private final BusinessRepository businesses;
    private final BranchRepository branches;
    private final BranchHourRepository hours;

    @Transactional
    public Business createBusiness(String name, String slug, BusinessCategory category, String phone, String email) {
        Business b = new Business();
        b.setName(name);
        b.setSlug(slug);
        b.setCategory(category);
        b.setPhone(phone);
        b.setEmail(email);
        businesses.save(b);
        Branch branch = new Branch();
        branch.setBusinessId(b.getId());
        branch.setName("Cơ sở chính");
        branch.setPhone(phone);
        branches.save(branch);
        createDefaultHours(branch.getId());
        return b;
    }

    public void createDefaultHours(Long branchId) {
        for (int d = 1; d <= 7; d++) {
            BranchHour h = new BranchHour();
            h.setBranchId(branchId);
            h.setDayOfWeek(d);
            h.setOpenTime(LocalTime.of(8, 0));
            h.setCloseTime(LocalTime.of(20, 0));
            hours.save(h);
        }
    }
}
