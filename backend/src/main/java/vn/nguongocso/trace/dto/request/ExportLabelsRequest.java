package vn.nguongocso.trace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Yêu cầu xuất tem QR cho lô hàng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportLabelsRequest {

    @Min(value = 0, message = "startIndex phải >= 0")
    @Builder.Default
    private int startIndex = 0;

    @NotNull(message = "count không được để trống")
    @Min(value = 1, message = "count phải >= 1")
    private Integer count;

    @NotBlank(message = "labelSize không được để trống")
    private String labelSize;

    private IncludeFields includeFields;

    /** Cờ bật/tắt các trường thông tin trên tem. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IncludeFields {

        @Builder.Default
        private boolean productName = true;

        @Builder.Default
        private boolean cooperativeName = true;

        @Builder.Default
        private boolean lotCode = true;

        @Builder.Default
        private boolean packagingDate = true;
    }
}
