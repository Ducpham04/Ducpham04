package vn.educenter.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByTenantIdOrderByNameAsc(Long tenantId);

    Optional<Room> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);

    List<Room> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);
}
