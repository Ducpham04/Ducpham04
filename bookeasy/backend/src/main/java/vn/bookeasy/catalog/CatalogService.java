package vn.bookeasy.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.bookeasy.catalog.CatalogDtos.*;
import vn.bookeasy.common.ApiException;
import vn.bookeasy.common.TextUtils;
import vn.bookeasy.staff.Staff;
import vn.bookeasy.staff.StaffRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogService {
    public static final int SLOT_MINUTES = 15;

    private final ServiceCategoryRepository categories;
    private final ServiceItemRepository services;
    private final StaffRepository staffRepo;

    // ---------- Danh mục ----------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long bid) {
        return categories.findByBusinessIdOrderBySortOrderAscIdAsc(bid).stream().map(CatalogService::toDto).toList();
    }

    @Transactional
    public CategoryResponse createCategory(Long bid, CategoryRequest req) {
        ServiceCategory c = new ServiceCategory();
        c.setBusinessId(bid);
        c.setName(req.name().trim());
        c.setSortOrder(req.sortOrder());
        return toDto(categories.save(c));
    }

    @Transactional
    public CategoryResponse updateCategory(Long bid, Long id, CategoryRequest req) {
        ServiceCategory c = categories.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("danh mục"));
        c.setName(req.name().trim());
        c.setSortOrder(req.sortOrder());
        return toDto(c);
    }

    @Transactional
    public void deleteCategory(Long bid, Long id) {
        ServiceCategory c = categories.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("danh mục"));
        services.findAllWithStaff(bid).stream().filter(s -> id.equals(s.getCategoryId())).forEach(s -> s.setCategoryId(null));
        categories.delete(c);
    }

    static CategoryResponse toDto(ServiceCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSortOrder());
    }

    // ---------- Dịch vụ ----------

    @Transactional(readOnly = true)
    public List<ServiceResponse> listServices(Long bid) {
        return services.findAllWithStaff(bid).stream().map(CatalogService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(Long bid, Long id) {
        return toDto(services.findWithStaff(id, bid).orElseThrow(() -> ApiException.notFound("dịch vụ")));
    }

    @Transactional
    public ServiceResponse createService(Long bid, ServiceRequest req) {
        ServiceItem s = new ServiceItem();
        s.setBusinessId(bid);
        apply(bid, s, req);
        return toDto(services.save(s));
    }

    @Transactional
    public ServiceResponse updateService(Long bid, Long id, ServiceRequest req) {
        ServiceItem s = services.findWithStaff(id, bid).orElseThrow(() -> ApiException.notFound("dịch vụ"));
        apply(bid, s, req);
        return toDto(s);
    }

    /** Xóa mềm: ẩn dịch vụ để giữ lịch sử booking. */
    @Transactional
    public void deleteService(Long bid, Long id) {
        ServiceItem s = services.findByIdAndBusinessId(id, bid).orElseThrow(() -> ApiException.notFound("dịch vụ"));
        s.setActive(false);
    }

    private void apply(Long bid, ServiceItem s, ServiceRequest req) {
        if (req.durationMinutes() % SLOT_MINUTES != 0) {
            throw ApiException.badRequest("INVALID_DURATION", "Thời lượng phải là bội số của 15 phút");
        }
        if (req.categoryId() != null) {
            categories.findByIdAndBusinessId(req.categoryId(), bid).orElseThrow(() -> ApiException.notFound("danh mục"));
        }
        s.setName(req.name().trim());
        s.setCategoryId(req.categoryId());
        s.setDescription(TextUtils.blankToNull(req.description()));
        s.setDurationMinutes(req.durationMinutes());
        s.setPrice(req.price());
        s.setActive(req.active());
        s.setSortOrder(req.sortOrder());
        Set<Staff> staff = new HashSet<>();
        if (req.staffIds() != null) {
            for (Long sid : new HashSet<>(req.staffIds())) {
                staff.add(staffRepo.findByIdAndBusinessId(sid, bid).orElseThrow(() -> ApiException.notFound("nhân viên")));
            }
        }
        s.getStaff().clear();
        s.getStaff().addAll(staff);
    }

    static ServiceResponse toDto(ServiceItem s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getCategoryId(), s.getDescription(), s.getDurationMinutes(),
                s.getPrice(), s.isActive(), s.getSortOrder(), s.getStaff().stream().map(Staff::getId).sorted().toList());
    }
}
