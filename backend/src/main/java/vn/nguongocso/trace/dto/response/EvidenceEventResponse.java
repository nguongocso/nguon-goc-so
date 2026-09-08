package vn.nguongocso.trace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response cho một sự kiện bằng chứng sản lượng thực (thu hoạch / sơ chế)
 * phục vụ form tạo yêu cầu cấp bổ sung dải mã truy xuất (NCL-04-CN-007).
 *
 * <p>FE hiển thị danh sách phẳng để VT-02 tick chọn, thay vì phải đi qua
 * lô sản xuất → lô hàng → timeline.</p>
 */
@Getter
@Setter
@Builder
public class EvidenceEventResponse {
    private UUID eventId;

    /** Tên enum loại sự kiện: HARVEST hoặc PREPROCESSING. */
    private String eventType;

    private LocalDateTime recordedAt;

    private String recordedByName;

    /** Lô hàng gắn kèm nếu sự kiện đã thuộc một lô hàng (nullable). */
    private UUID shipmentId;

    /** Lô sản xuất của sự kiện (từ lô hàng hoặc eventData, nullable). */
    private UUID productionLotId;

    private String productionLotName;

    /**
     * Số lượng sản lượng thực ghi nhận trong sự kiện.
     * Thu hoạch (HARVEST): trường "quantity"; sơ chế (PREPROCESSING): ưu tiên
     * "outputQuantity" rồi tới "inputQuantity" trong eventData của ChainEvent.
     */
    private BigDecimal quantity;
}
