package vn.nguongocso.report.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDossierExportRequest {

    @NotEmpty(message = "Danh sách lô hàng được chọn không được để trống.")
    private List<UUID> shipmentIds;

    private String title;

    private String note;

    /** ID mẫu hồ sơ áp dụng (tùy chọn - NCL-07-CN-007). Bỏ trống sẽ dùng mẫu mặc định của tổ chức hoặc bộ trường chuẩn. */
    private UUID templateId;
}
