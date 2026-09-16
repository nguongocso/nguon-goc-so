package vn.nguongocso.integration.apikey.event;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Sự kiện lượt gọi chạm ngưỡng cảnh báo hạn mức của khóa truy cập (NCL-12-CN-005).
 * <p>
 * Được phát ngay trong luồng kiểm tra rate-limit khi số lượt gọi trong ngày hiện tại
 * vừa chạm ngưỡng cấu hình. Listener chỉ gửi thông báo (không chặn request đối tác).
 */
@Getter
@Builder
public class ApiKeyQuotaThresholdEvent {

    /** ID khóa truy cập (dùng làm entityId chống trùng thông báo). */
    private UUID apiKeyId;

    /** Tổ chức sở hữu khóa (phạm vi người nhận thông báo). */
    private UUID organizationId;

    /** Tên đối tác hiển thị trong nội dung cảnh báo. */
    private String partnerName;

    /** Hạn mức lượt gọi mỗi giờ của khóa. */
    private int rateLimitPerHour;

    /** Số lượt đã gọi trong ngày hôm nay (đã chạm ngưỡng cảnh báo). */
    private int usedCalls;

    /** Ngưỡng lượt gọi trong ngày kích hoạt cảnh báo của khóa. */
    private int warningThreshold;
}
