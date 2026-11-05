package vn.educenter.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    Page<NotificationLog> findByTenantIdOrderByIdDesc(Long tenantId, Pageable pageable);

    boolean existsByRefKeyAndChannelAndStatusIn(String refKey, NotificationLog.Channel channel, java.util.Collection<NotificationLog.Status> statuses);
}
