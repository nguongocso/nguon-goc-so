package vn.nguongocso.certification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.nguongocso.certification.enums.InspectionResultEntryLinkStatus;

/**
 * DTO phản hồi thông tin liên kết nhập kết quả kiểm nghiệm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InspectionResultEntryLinkResponse {

    /**
     * ID của liên kết.
     */
    private UUID id;

    /**
     * Trạng thái của liên kết (ACTIVE, USED, REVOKED, EXPIRED).
     */
    private InspectionResultEntryLinkStatus status;

    /**
     * Email người nhận đã được gửi liên kết.
     */
    private String recipientEmail;

    /**
     * Tiền tố của token dùng để quản lý/nhận diện mà không làm lộ secret.
     */
    private String tokenPrefix;

    /**
     * Thời điểm hết hiệu lực của liên kết.
     */
    private LocalDateTime expiresAt;

    /**
     * Thời điểm liên kết được sử dụng để gửi kết quả.
     */
    private LocalDateTime usedAt;

    /**
     * Thời điểm tạo liên kết.
     */
    private LocalDateTime createdAt;

    /**
     * Đường dẫn cổng nhập kết quả kèm token bí mật, chỉ trả về đúng một lần duy nhất lúc tạo mới.
     */
    private String entryUrl;
}
