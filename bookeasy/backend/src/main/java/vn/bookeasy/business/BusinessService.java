package vn.bookeasy.business;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.bookeasy.business.BusinessDtos.*;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.config.AppProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final BusinessRepository businesses;
    private final BranchRepository branches;
    private final BranchHourRepository hours;
    private final HolidayRepository holidays;
    private final BusinessSetupService setup;
    private final BusinessMapper mapper;
    private final AppProperties props;

    public Business require(Long businessId) {
        return businesses.findById(businessId).orElseThrow(() -> ApiException.notFound("cơ sở"));
    }

    @Transactional(readOnly = true)
    public BusinessResponse get(Long businessId) {
        return mapper.toResponse(require(businessId));
    }

    @Transactional
    public BusinessResponse update(Long businessId, UpdateBusinessRequest req) {
        Business b = require(businessId);
        if (req.paymentMode() != PaymentMode.NONE
                && (TextUtils.blankToNull(req.bankBin()) == null || TextUtils.blankToNull(req.bankAccountNo()) == null)) {
            throw ApiException.badRequest("BANK_REQUIRED", "Cần nhập ngân hàng và số tài khoản để thu cọc/trả trước");
        }
        b.setName(req.name().trim());
        b.setDescription(TextUtils.blankToNull(req.description()));
        b.setPhone(TextUtils.blankToNull(req.phone()));
        b.setEmail(TextUtils.blankToNull(req.email()));
        b.setAddress(TextUtils.blankToNull(req.address()));
        b.setLogoUrl(TextUtils.blankToNull(req.logoUrl()));
        b.setBrandColor(req.brandColor().toLowerCase());
        if (req.category() != null) b.setCategory(req.category());
        b.setPaymentMode(req.paymentMode());
        b.setDepositPercent(req.depositPercent());
        b.setBankBin(TextUtils.blankToNull(req.bankBin()));
        b.setBankAccountNo(TextUtils.blankToNull(req.bankAccountNo()));
        b.setBankAccountName(TextUtils.blankToNull(req.bankAccountName()) == null ? null : req.bankAccountName().trim().toUpperCase());
        b.setMinLeadMinutes(req.minLeadMinutes());
        b.setBookingWindowDays(req.bookingWindowDays());
        b.setCancelBeforeMinutes(req.cancelBeforeMinutes());
        return mapper.toResponse(b);
    }

    @Transactional
    public BusinessResponse uploadLogo(Long businessId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("FILE_REQUIRED", "Vui lòng chọn ảnh");
        if (file.getSize() > MAX_LOGO_BYTES) throw ApiException.badRequest("FILE_TOO_LARGE", "Ảnh vượt quá 2MB");
        String ext;
        try (InputStream in = file.getInputStream()) {
            ext = detectImage(in.readNBytes(12));
        } catch (IOException e) {
            throw ApiException.badRequest("FILE_INVALID", "Không đọc được ảnh");
        }
        if (ext == null) throw ApiException.badRequest("FILE_INVALID", "Chỉ hỗ trợ ảnh PNG, JPG hoặc WEBP");
        try {
            Path dir = Path.of(props.uploadDir()).toAbsolutePath();
            Files.createDirectories(dir);
            String name = "logo-" + businessId + "-" + UUID.randomUUID() + "." + ext;
            file.transferTo(dir.resolve(name));
            Business b = require(businessId);
            b.setLogoUrl("/uploads/" + name);
            return mapper.toResponse(b);
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh", e);
        }
    }

    /** Kiểm tra magic bytes thay vì tin Content-Type của client. */
    static String detectImage(byte[] h) {
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        return null;
    }

    // ---------- Chi nhánh ----------

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(Long businessId) {
        return branches.findByBusinessIdOrderByIdAsc(businessId).stream().map(this::toBranchResponse).toList();
    }

    public BranchResponse toBranchResponse(Branch br) {
        List<BranchHourDto> hs = hours.findByBranchIdOrderByDayOfWeekAsc(br.getId()).stream().map(mapper::toDto).toList();
        return mapper.toResponse(br, hs);
    }

    public Branch requireBranch(Long businessId, Long branchId) {
        return branches.findByIdAndBusinessId(branchId, businessId).orElseThrow(() -> ApiException.notFound("chi nhánh"));
    }

    @Transactional
    public BranchResponse createBranch(Long businessId, BranchRequest req) {
        Branch br = new Branch();
        br.setBusinessId(businessId);
        applyBranch(br, req);
        branches.save(br);
        setup.createDefaultHours(br.getId());
        return toBranchResponse(br);
    }

    @Transactional
    public BranchResponse updateBranch(Long businessId, Long id, BranchRequest req) {
        Branch br = requireBranch(businessId, id);
        applyBranch(br, req);
        if (!br.isActive() && branches.findByBusinessIdAndActiveTrueOrderByIdAsc(businessId).stream().noneMatch(b -> !b.getId().equals(id))) {
            throw ApiException.badRequest("LAST_BRANCH", "Cơ sở cần ít nhất một chi nhánh đang hoạt động");
        }
        return toBranchResponse(br);
    }

    private void applyBranch(Branch br, BranchRequest req) {
        br.setName(req.name().trim());
        br.setAddress(TextUtils.blankToNull(req.address()));
        br.setPhone(TextUtils.blankToNull(req.phone()));
        if (req.active() != null) br.setActive(req.active());
    }

    @Transactional
    public BranchResponse updateHours(Long businessId, Long branchId, HoursRequest req) {
        Branch br = requireBranch(businessId, branchId);
        long distinct = req.hours().stream().map(BranchHourDto::dayOfWeek).distinct().count();
        if (distinct != 7) throw ApiException.badRequest("INVALID_HOURS", "Cần cấu hình đủ 7 ngày trong tuần");
        for (BranchHourDto h : req.hours()) {
            if (!h.closed() && !h.openTime().isBefore(h.closeTime())) {
                throw ApiException.badRequest("INVALID_HOURS", "Giờ mở cửa phải trước giờ đóng cửa");
            }
        }
        hours.deleteByBranchId(branchId);
        hours.flush();
        req.hours().stream().sorted(Comparator.comparingInt(BranchHourDto::dayOfWeek)).forEach(dto -> {
            BranchHour h = new BranchHour();
            h.setBranchId(branchId);
            h.setDayOfWeek(dto.dayOfWeek());
            h.setOpenTime(dto.openTime());
            h.setCloseTime(dto.closeTime());
            h.setClosed(dto.closed());
            hours.save(h);
        });
        return toBranchResponse(br);
    }

    // ---------- Ngày nghỉ ----------

    @Transactional(readOnly = true)
    public List<HolidayResponse> listHolidays(Long businessId, java.time.LocalDate from) {
        return holidays.findByBusinessIdAndHolidayDateGreaterThanEqualOrderByHolidayDateAsc(businessId, from)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public HolidayResponse addHoliday(Long businessId, HolidayRequest req) {
        if (req.branchId() != null) requireBranch(businessId, req.branchId());
        Holiday h = new Holiday();
        h.setBusinessId(businessId);
        h.setBranchId(req.branchId());
        h.setHolidayDate(req.date());
        h.setName(req.name().trim());
        return mapper.toResponse(holidays.save(h));
    }

    @Transactional
    public void deleteHoliday(Long businessId, Long id) {
        holidays.delete(holidays.findByIdAndBusinessId(id, businessId).orElseThrow(() -> ApiException.notFound("ngày nghỉ")));
    }
}
