package vn.nguongocso.alert.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;

/** Repository thao tác Alert. */
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    /** Lọc theo loại cảnh báo. */
    Page<Alert> findByType(
            AlertType type,
            Pageable pageable);

    /** Lọc theo loại và trạng thái. */
    Page<Alert> findByTypeAndStatus(
            AlertType type,
            AlertStatus status,
            Pageable pageable);

    /** Lọc theo loại và thời gian tạo. */
    Page<Alert> findByTypeAndCreatedAtBetween(
            AlertType type,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    /** Lọc theo loại, trạng thái và thời gian tạo. */
    Page<Alert> findByTypeAndStatusAndCreatedAtBetween(
            AlertType type,
            AlertStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM Alert a
            WHERE (:type IS NULL OR a.type = :type)
              AND (:status IS NULL OR a.status = :status)
              AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
              AND (:toDate IS NULL OR a.createdAt <= :toDate)
              AND (:organizationId IS NULL
                   OR a.organization.organizationId = :organizationId)
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> searchAlerts(
            @Param("type") AlertType type,
            @Param("status") AlertStatus status,
            @Param("organizationId") UUID organizationId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    /**
     * Kiểm tra đã tồn tại cảnh báo đang chờ xử lý của mã truy xuất hay chưa.
     */
    boolean existsByRelatedEntityIdAndTypeAndStatus(
            UUID relatedEntityId,
            AlertType type,
            AlertStatus status);

    java.util.List<Alert> findByRelatedEntityIdAndTypeAndStatus(
            UUID relatedEntityId,
            AlertType type,
            AlertStatus status);
}