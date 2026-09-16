package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Kết quả tạo lô sản xuất mới từ mẫu vụ trước (NCL-02-CN-007).
 */
@Getter
@Setter
@Builder
public class CloneProductionLotResponse {
    /** Lô sản xuất mới vừa được tạo (luôn ở trạng thái DRAFT). */
    private CreateProductionLotResponse lot;

    /** Các chứng nhận còn hiệu lực đã được sao chép sang lô mới. */
    private List<CloneCertificationInfo> copiedCertifications;

    /** Các chứng nhận hết hạn / bị từ chối đã bị bỏ qua. */
    private List<CloneCertificationInfo> skippedCertifications;

    /** Cảnh báo clone để hiển thị trên giao diện. */
    private List<String> warnings;
}
