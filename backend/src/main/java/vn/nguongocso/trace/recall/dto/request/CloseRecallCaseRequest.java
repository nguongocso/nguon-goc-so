package vn.nguongocso.trace.recall.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.nguongocso.trace.recall.enums.LotResolution;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Yêu cầu kết thúc vụ việc thu hồi (NCL-08-CN-012).
 */
@Data
public class CloseRecallCaseRequest {

    /** Biện pháp khắc phục phòng ngừa chung — bắt buộc, không được rỗng (QTN-27). */
    @NotBlank(message = "Biện pháp khắc phục phòng ngừa không được để trống.")
    private String remediationMeasures;

    /** Danh sách ID tệp biên bản/bằng chứng đã upload (tùy chọn). */
    private List<UUID> evidenceFileIds;

    /** Kết quả xử lý từng lô — bắt buộc và phải phủ hết các lô trong vụ việc. */
    @NotEmpty(message = "Danh sách kết quả xử lý lô không được để trống.")
    private List<LotResultItem> lotResults;

    /** Kết quả xử lý của một lô hàng trong vụ việc. */
    @Data
    public static class LotResultItem {

        @NotNull(message = "ID lô hàng là bắt buộc.")
        private UUID shipmentId;

        @NotNull(message = "Kết quả xử lý là bắt buộc.")
        private LotResolution resolution;

        /**
         * Số lượng thực tế thu hồi được. Bắt buộc, ≥ 0 và ≤ lô.totalQuantity
         * (kiểm tra trong service vì phụ thuộc dữ liệu lô).
         */
        @NotNull(message = "Số lượng thu hồi được là bắt buộc.")
        @DecimalMin(value = "0.0", message = "Số lượng thu hồi không được âm.")
        private BigDecimal recoveredQuantity;

        /** Ghi chú; bắt buộc khi resolution = UNRECOVERABLE (lý do + biện pháp rủi ro). */
        private String notes;
    }
}
