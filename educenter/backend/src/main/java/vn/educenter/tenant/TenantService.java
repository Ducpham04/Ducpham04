package vn.educenter.tenant;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.common.BusinessException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.CurrentUser;
import vn.educenter.security.Role;
import vn.educenter.student.StudentRepository;
import vn.educenter.user.User;
import vn.educenter.user.UserRepository;
import vn.educenter.user.UserService;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final UserService userService;
    private final TenantMapper mapper;

    @Transactional(readOnly = true)
    public List<TenantDtos.TenantResponse> listAll() {
        return tenantRepository.findAllByOrderByCreatedAtDesc().stream().map(this::withCounts).toList();
    }

    @Transactional
    public TenantDtos.TenantResponse create(TenantDtos.CreateTenantRequest req) {
        String slug = req.slug().toLowerCase(Locale.ROOT);
        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("Mã trung tâm \"" + slug + "\" đã tồn tại");
        }
        Tenant t = new Tenant();
        t.setName(req.name().trim());
        t.setSlug(slug);
        t.setPhone(req.phone());
        t.setEmail(req.email());
        t.setAddress(req.address());
        t.setPlan(req.plan());
        t.setStatus(Tenant.Status.ACTIVE);
        tenantRepository.save(t);
        User owner = userService.newUser(t.getId(), req.owner().email(), req.owner().password(),
                req.owner().fullName(), Role.CENTER_ADMIN);
        userRepository.save(owner);
        return withCounts(t);
    }

    @Transactional
    public TenantDtos.TenantResponse update(Long id, TenantDtos.UpdateTenantRequest req) {
        Tenant t = tenantRepository.findById(id).orElseThrow(() -> NotFoundException.of("trung tâm"));
        t.setName(req.name().trim());
        t.setPhone(req.phone());
        t.setEmail(req.email());
        t.setAddress(req.address());
        t.setPlan(req.plan());
        return withCounts(t);
    }

    @Transactional
    public TenantDtos.TenantResponse setStatus(Long id, Tenant.Status status) {
        Tenant t = tenantRepository.findById(id).orElseThrow(() -> NotFoundException.of("trung tâm"));
        t.setStatus(status);
        return withCounts(t);
    }

    @Transactional(readOnly = true)
    public TenantDtos.TenantResponse currentCenter() {
        return mapper.toResponse(current());
    }

    @Transactional
    public TenantDtos.TenantResponse updateCurrentCenter(TenantDtos.CenterRequest req) {
        Tenant t = current();
        mapper.updateCenter(req, t);
        t.setName(req.name().trim());
        return mapper.toResponse(t);
    }

    public Tenant current() {
        return tenantRepository.findById(CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("trung tâm"));
    }

    private TenantDtos.TenantResponse withCounts(Tenant t) {
        TenantDtos.TenantResponse r = mapper.toResponse(t);
        return new TenantDtos.TenantResponse(r.id(), r.name(), r.slug(), r.phone(), r.email(), r.address(), r.plan(),
                r.status(), r.bankCode(), r.bankAccountNo(), r.bankAccountName(), r.createdAt(),
                studentRepository.countByTenantId(t.getId()), userRepository.countByTenantId(t.getId()));
    }
}
