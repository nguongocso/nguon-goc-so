package vn.nguongocso.ai.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tổng hợp thông tin về lưu thông lô hàng và tiến độ bàn giao.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentSummaryDto {
    /** Số lượng lô hàng đang trong quá trình lưu thông/vận chuyển (ACTIVATED). */
    private long inTransitShipmentsCount;

    /** Số lượng biên bản bàn giao đang chờ đối tác xác nhận tiếp nhận (PENDING_CONFIRMATION). */
    private long pendingHandoverCount;
}
