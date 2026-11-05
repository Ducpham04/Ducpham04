package vn.educenter.catalog;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.educenter.common.BusinessException;
import vn.educenter.common.NotFoundException;
import vn.educenter.security.CurrentUser;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CourseRepository courseRepository;
    private final RoomRepository roomRepository;
    private final CatalogMapper mapper;

    @Transactional(readOnly = true)
    public List<CatalogDtos.CourseResponse> courses() {
        return courseRepository.findByTenantIdOrderByNameAsc(CurrentUser.tenantId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CatalogDtos.CourseResponse createCourse(CatalogDtos.CourseRequest req) {
        Long tenantId = CurrentUser.tenantId();
        if (courseRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, req.code())) {
            throw new BusinessException("Mã khóa học " + req.code() + " đã tồn tại");
        }
        Course c = new Course();
        c.setTenantId(tenantId);
        apply(c, req);
        return mapper.toResponse(courseRepository.save(c));
    }

    @Transactional
    public CatalogDtos.CourseResponse updateCourse(Long id, CatalogDtos.CourseRequest req) {
        Course c = findCourse(id);
        if (courseRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(c.getTenantId(), req.code(), id)) {
            throw new BusinessException("Mã khóa học " + req.code() + " đã tồn tại");
        }
        apply(c, req);
        return mapper.toResponse(c);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.delete(findCourse(id));
        courseRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.RoomResponse> rooms() {
        return roomRepository.findByTenantIdOrderByNameAsc(CurrentUser.tenantId()).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public CatalogDtos.RoomResponse createRoom(CatalogDtos.RoomRequest req) {
        Long tenantId = CurrentUser.tenantId();
        if (roomRepository.existsByTenantIdAndNameIgnoreCase(tenantId, req.name().trim())) {
            throw new BusinessException("Phòng " + req.name() + " đã tồn tại");
        }
        Room r = new Room();
        r.setTenantId(tenantId);
        r.setName(req.name().trim());
        r.setCapacity(req.capacity());
        r.setActive(req.active() == null || req.active());
        return mapper.toResponse(roomRepository.save(r));
    }

    @Transactional
    public CatalogDtos.RoomResponse updateRoom(Long id, CatalogDtos.RoomRequest req) {
        Room r = findRoom(id);
        if (roomRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(r.getTenantId(), req.name().trim(), id)) {
            throw new BusinessException("Phòng " + req.name() + " đã tồn tại");
        }
        r.setName(req.name().trim());
        r.setCapacity(req.capacity());
        if (req.active() != null) r.setActive(req.active());
        return mapper.toResponse(r);
    }

    @Transactional
    public void deleteRoom(Long id) {
        roomRepository.delete(findRoom(id));
        roomRepository.flush();
    }

    public Course findCourse(Long id) {
        return courseRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("khóa học"));
    }

    public Room findRoom(Long id) {
        return roomRepository.findByIdAndTenantId(id, CurrentUser.tenantId()).orElseThrow(() -> NotFoundException.of("phòng học"));
    }

    private static void apply(Course c, CatalogDtos.CourseRequest req) {
        c.setCode(req.code().trim().toUpperCase());
        c.setName(req.name().trim());
        c.setLevel(req.level());
        c.setTotalSessions(req.totalSessions());
        c.setTuitionFee(req.tuitionFee());
        c.setDescription(req.description());
        if (req.active() != null) c.setActive(req.active());
    }
}
