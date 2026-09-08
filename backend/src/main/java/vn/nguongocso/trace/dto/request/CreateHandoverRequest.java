package vn.nguongocso.trace.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để tạo phiếu bàn giao mới.
 */
@Getter
@Setter
public class CreateHandoverRequest {

    @NotNull(message = "ID lô hàng không được để trống")
    private UUID shipmentId;

    @NotNull(message = "ID tổ chức nhận không được để trống")
    private UUID toOrganizationId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Long quantity;

    private LocalDateTime plannedAt;

    private String vehicleInfo;

    private String carrierName;

    private String note;
}
