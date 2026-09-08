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
}
