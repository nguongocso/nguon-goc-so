package vn.nguongocso.event.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO ghi nhận sự kiện thu mua.
 *
 * @author Team WEB 1
 */
@Getter
@Setter
public class RecordProcurementEventRequest {

    @NotNull(message = "Vui lòng chọn lô hàng")
    private UUID shipmentId;

    @NotNull(message = "Số lượng nhận không được để trống")
    @Positive(message = "Số lượng nhận phải lớn hơn 0")
    private Long receivedQuantity;

    private String notes;

    private Double latitude;

    private Double longitude;

}
