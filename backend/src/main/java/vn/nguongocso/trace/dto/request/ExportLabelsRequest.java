package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request xuất tem QR cho lô hàng (NCL-04-CN-005).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportLabelsRequest {

    /** Chỉ số bắt đầu trong danh sách mã đã sinh (mặc định 0). */
    @Min(value = 0, message = "startIndex phải >= 0")
    private int startIndex = 0;

    /** Số tem cần xuất. */
    @NotNull(message = "count không được để trống")
    @Min(value = 1, message = "count phải >= 1")
    private Integer count;

    /** Khổ tem, ví dụ "40x30", "50x40", "70x50" (mm). */
    @NotBlank(message = "labelSize không được để trống")
    private String labelSize;

    /** Các trường tùy chọn in trên tem (mặc định tất cả là true). */
    private IncludeFields includeFields;

    /**
     * Cờ bật/tắt các trường thông tin tùy chọn trên tem.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IncludeFields {
        private boolean productName = true;
        private boolean cooperativeName = true;
        private boolean lotCode = true;
        private boolean packagingDate = true;
    }
}
