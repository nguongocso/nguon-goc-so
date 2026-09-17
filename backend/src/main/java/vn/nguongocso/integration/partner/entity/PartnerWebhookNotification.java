package vn.nguongocso.integration.partner.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.integration.apikey.entity.PartnerApiKey;
import vn.nguongocso.integration.partner.enums.WebhookDeliveryStatus;
import vn.nguongocso.trace.entity.Shipment;

/**
 * Thực thể lưu trữ lịch sử gửi thông báo Webhook thu hồi lô tới bên thứ ba (NCL-12-CN-006).
 * <p>
 * Lưu trữ trạng thái gửi, số lần thử lại theo lịch giãn dần và toàn bộ nhật ký các lần gửi trong
 * trường {@code attemptsLog} định dạng JSON.
 */
@Entity
@Table(
    name = "partner_webhook_notifications",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uq_pwn_key_shipment_status",
            columnNames = {"partner_api_key_id", "shipment_id", "new_status"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerWebhookNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_api_key_id", nullable = false)
    private PartnerApiKey partnerApiKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "lot_code", nullable = false, length = 100)
    private String lotCode;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "public_reason", nullable = false, columnDefinition = "TEXT")
    private String publicReason;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private WebhookDeliveryStatus deliveryStatus;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "attempts_log", columnDefinition = "json")
    private String attemptsLog;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (maxAttempts == null) {
            maxAttempts = 5;
        }
        if (deliveryStatus == null) {
            deliveryStatus = WebhookDeliveryStatus.PENDING_RETRY;
        }
    }
}
