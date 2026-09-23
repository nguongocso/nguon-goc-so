package vn.nguongocso.integration.partner.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.nguongocso.integration.partner.entity.PartnerWebhookNotification;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;

/**
 * Repository quản lý thông báo Webhook gửi tới đối tác.
*/
@Repository
public interface PartnerWebhookNotificationRepository extends JpaRepository<PartnerWebhookNotification, UUID> {
    /**
     * Lấy danh sách thông báo theo khóa API đối tác.
     */
    Page<PartnerWebhookNotification> findByPartnerApiKey_IdOrderByCreatedAtDesc(UUID partnerApiKeyId,
            Pageable pageable);

    /**
     * Lấy danh sách thông báo theo khóa API đối tác và trạng thái phân phối.
     */
    Page<PartnerWebhookNotification> findByPartnerApiKey_IdAndDeliveryStatusOrderByCreatedAtDesc(
            UUID partnerApiKeyId, WebhookDeliveryStatus deliveryStatus, Pageable pageable);

    /**
     * Tìm các thông báo đang chờ thử lại mà thời điểm hẹn gửi lại đã đến.
     */
    List<PartnerWebhookNotification> findByDeliveryStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            WebhookDeliveryStatus deliveryStatus, LocalDateTime now);

    /**
     * Đếm tổng số thông báo theo trạng thái gửi của một khóa API.
     */
    long countByPartnerApiKey_IdAndDeliveryStatus(UUID partnerApiKeyId, WebhookDeliveryStatus deliveryStatus);

    /**
     * Kiểm tra thông báo thu hồi đã được phát cho đối tác hay chưa.
     */
    boolean existsByPartnerApiKey_IdAndShipment_IdAndNewStatus(
            UUID partnerApiKeyId, UUID shipmentId, String newStatus);

    /**
     * Hủy bỏ toàn bộ các thông báo đang chờ thử lại của một khóa API khi khóa bị thu hồi.
     */
    @Modifying
    @Query("""
            UPDATE PartnerWebhookNotification n
            SET n.deliveryStatus = vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus.CANCELLED,
                n.nextRetryAt = null,
                n.completedAt = :now,
                n.lastErrorMessage = :reason
            WHERE n.partnerApiKey.id = :partnerApiKeyId
              AND n.deliveryStatus = vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus.PENDING_RETRY
            """)
    int cancelPendingNotificationsForApiKey(
            @Param("partnerApiKeyId") UUID partnerApiKeyId,
            @Param("reason") String reason,
            @Param("now") LocalDateTime now);
}
