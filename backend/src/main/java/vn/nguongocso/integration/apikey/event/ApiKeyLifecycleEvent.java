package vn.nguongocso.integration.apikey.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.integration.apikey.enums.PartnerApiKeyStatus;

/**
 * Sự kiện vòng đời khóa truy cập vừa được cấp hoặc gia hạn (NCL-12-CN-005).
 * <p>
 * Được phát sau khi {@code PartnerApiKeyService} lưu khóa mới (khóa Live, khóa
 * thử nghiệm) hoặc gia hạn khóa hiện có. Listener chỉ gửi thông báo khi khóa ở
 * trạng thái {@code ACTIVE} và thời hạn nằm trong ngưỡng cảnh báo
 * ({@code 0 < expiresAt - now <= expiryWarningDays}); các trường hợp còn lại bị
 * bỏ qua để lần quét 00:00 hằng ngày xử lý (hoặc không cần cảnh báo).
 * <p>
 * Dùng sự kiện thay vì gọi trực tiếp {@code ApiKeyWarningService} để tránh phụ
 * thuộc vòng tròn (dịch vụ cảnh báo đang cần {@code PartnerApiKeyService} cho
 * bộ đếm hạn mức theo giờ).
 */
@Getter
@Builder
public class ApiKeyLifecycleEvent {

    /** ID khóa truy cập vừa được cấp hoặc gia hạn (dùng làm entityId chống trùng). */
    private UUID apiKeyId;

    /** Tổ chức sở hữu khóa (phạm vi người nhận thông báo). */
    private UUID organizationId;

    /** Tên đối tác hiển thị trong nội dung cảnh báo. */
    private String partnerName;

    /** Thời điểm hết hạn mới của khóa. */
    private LocalDateTime expiresAt;

    /** Trạng thái của khóa sau khi cấp hoặc gia hạn. */
    private PartnerApiKeyStatus status;
}
