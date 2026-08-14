package vn.nguongocso.event.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.nguongocso.event.entity.OfflineSyncLog;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository cho thực thể OfflineSyncLog.
 */
@Repository
public interface OfflineSyncLogRepository extends JpaRepository<OfflineSyncLog, UUID> {

    // Tìm kiếm log theo ID sự kiện ngoại tuyến để phục vụ kiểm tra trùng
    Optional<OfflineSyncLog> findByOfflineEventId(UUID offlineEventId);

    /**
     * Tìm log theo offline_event_id và khóa pessimistic (FOR UPDATE).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM OfflineSyncLog l WHERE l.offlineEventId = :offlineEventId")
    Optional<OfflineSyncLog> findByOfflineEventIdWithLock(@Param("offlineEventId") UUID offlineEventId);
}
