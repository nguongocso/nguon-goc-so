package vn.nguongocso.event.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * DTO phản hồi kiểm tra tính hợp lệ của lô trước khi tạo sự kiện.
 *
 * @author Triệu Văn Đại
 */
@Data
@Builder
public class LotValidationResponse {
    private UUID lotId;

    private String eventType;

    private boolean valid;

    private String message;

    private LotDetails details;

    /**
     * Chi tiết của lô hàng.
     */
    @Data
    @Builder
    public static class LotDetails {
        private String lotType; // "PRODUCTION_LOT" or "SHIPMENT"
        private String currentStatus;
        private UUID organizationId;
    }
}
