package vn.nguongocso.report.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Yêu cầu kiểm tra tính đầy đủ hồ sơ theo lô. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDossierCheckRequest {

    @NotEmpty(message = "Danh sách lô hàng không được để trống.")
    private List<UUID> shipmentIds;
}
