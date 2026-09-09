package vn.nguongocso.recall.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để tạo yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (NCL-08-CN-011).
 */
@Getter
@Setter
public class CreateBulkRecallRequest {

    /**
     * ID lô sản xuất nguồn (bắt buộc).
     */
    @NotNull(message = "ID lô sản xuất không được để trống.")
    private UUID productionLotId;

    /**
     * Lý do thu hồi chung cho vụ việc (bắt buộc).
     */
    @NotBlank(message = "Lý do thu hồi không được để trống.")
    private String reason;

    /**
     * Bằng chứng/minh chứng (tùy chọn).
     */
    private String evidence;

    /**
     * Danh sách ID lô hàng được chọn để thu hồi (phải có ít nhất 1).
     */
    @NotNull(message = "Danh sách lô hàng thuộc phạm vi không được để trống.")
    private List<UUID> includedShipmentIds;

    /**
     * Danh sách lô hàng bị loại khỏi phạm vi kèm lý do.
     */
    private List<ExcludedShipment> excludedShipments;

    /**
     * DTO nội bộ cho lô hàng bị loại.
     */
    @Getter
    @Setter
    public static class ExcludedShipment {
        /**
         * ID lô hàng bị loại.
         */
        @NotNull(message = "ID lô hàng không được để trống.")
        private UUID shipmentId;

        /**
         * Lý do loại bỏ (bắt buộc).
         */
        @NotBlank(message = "Lý do loại bỏ không được để trống.")
        private String exclusionReason;
    }
}
