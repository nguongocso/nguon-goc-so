package vn.nguongocso.notification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.alert.enums.NotificationType;

/**
 * DTO phản hồi chi tiết một thông báo.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;

    private NotificationType type;

    private String title;

    private String content;

    /**
     * ID thực thể nghiệp vụ liên kết (VD: phiếu bàn giao lô hàng).
     * Null khi thông báo không có thực thể đính kèm.
     */
    private UUID entityId;

    private Boolean isRead;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
