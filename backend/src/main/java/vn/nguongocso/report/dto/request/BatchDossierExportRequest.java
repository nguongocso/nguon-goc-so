package vn.nguongocso.report.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu xuất bộ hồ sơ theo lô. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDossierExportRequest {
    @NotEmpty(message = "Danh sách lô hàng được chọn không được để trống.")
    @Size(min = 1, max = 20, message = "Mỗi bộ hồ sơ xuất tối đa 20 lô hàng")
    private List<UUID> shipmentIds;

    private String title;

    private String note;

    private UUID templateId;
}
