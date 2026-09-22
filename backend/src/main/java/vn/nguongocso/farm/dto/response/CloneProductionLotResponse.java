package vn.nguongocso.farm.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Kết quả tạo lô sản xuất mới từ mẫu vụ trước.
*/
@Getter
@Setter
@Builder
public class CloneProductionLotResponse {
    private CreateProductionLotResponse lot;

    private List<CloneCertificationInfo> copiedCertifications;

    private List<CloneCertificationInfo> skippedCertifications;

    private List<String> warnings;
}
