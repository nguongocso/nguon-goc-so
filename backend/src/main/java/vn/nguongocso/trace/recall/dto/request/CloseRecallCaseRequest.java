package vn.nguongocso.trace.recall.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.nguongocso.trace.recall.enums.LotResolution;

/** Yêu cầu kết thúc vụ việc thu hồi. */
@Data
public class CloseRecallCaseRequest {
    @NotBlank(message = "Biện pháp khắc phục phòng ngừa không được để trống.")
    private String remediationMeasures;

    @Size(max = 5, message = "Chỉ được đính kèm tối đa 5 tệp biên bản.")
    private List<UUID> evidenceFileIds;

    @NotEmpty(message = "Danh sách kết quả xử lý lô không được để trống.")
    private List<LotResultItem> lotResults;

    /** Kết quả xử lý của một lô hàng trong vụ việc. */
    @Data
    public static class LotResultItem {

        @NotNull(message = "ID lô hàng là bắt buộc.")
        private UUID shipmentId;

        @NotNull(message = "Kết quả xử lý là bắt buộc.")
        private LotResolution resolution;

        @NotNull(message = "Số lượng thu hồi được là bắt buộc.")
        @DecimalMin(value = "0.0", message = "Số lượng thu hồi không được âm.")
        private BigDecimal recoveredQuantity;

        private String notes;
    }
}
