package vn.nguongocso.recall.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Yêu cầu tạo mới thu hồi hàng loạt. */
@Getter
@Setter
public class CreateBulkRecallRequest {

    @NotNull(message = "ID lô sản xuất không được để trống.")
    private UUID productionLotId;

    @NotBlank(message = "Lý do thu hồi không được để trống.")
    private String reason;

    private String evidence;

    @NotNull(message = "Danh sách lô hàng thuộc phạm vi không được để trống.")
    private List<UUID> includedShipmentIds;

    private List<ExcludedShipment> excludedShipments;

    /** Thông tin lô hàng bị loại khỏi phạm vi thu hồi. */
    @Getter
    @Setter
    public static class ExcludedShipment {

        @NotNull(message = "ID lô hàng không được để trống.")
        private UUID shipmentId;

        @NotBlank(message = "Lý do loại bỏ không được để trống.")
        private String exclusionReason;
    }
}
