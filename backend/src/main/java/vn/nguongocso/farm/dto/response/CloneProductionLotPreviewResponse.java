package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu xem trước khi tạo lô sản xuất mới từ mẫu vụ trước (NCL-02-CN-007).
 *
 * <p>
 * Chỉ chứa dữ liệu nền cần cho form tạo lô mới; không expose lịch sử vận
 * hành (nhật ký canh tác, sự kiện chuỗi, lô hàng, mã truy xuất) của lô mẫu.
 * </p>
 */
@Getter
@Setter
@Builder
public class CloneProductionLotPreviewResponse {
    private UUID sourceLotId;

    private String sourceLotName;

    private UUID farmAreaId;

    private String farmAreaName;

    private UUID productCategoryId;

    private String productCategoryName;

    private String name;

    private Double expectedQuantity;

    private String expectedQuantityUnit;

    private LocalDate plantingDate;

    /** Các chứng nhận còn hiệu lực sẽ được sao chép sang lô mới. */
    private List<CloneCertificationInfo> activeCertifications;

    /** Các chứng nhận hết hạn / bị từ chối sẽ bị bỏ qua khi sao chép. */
    private List<CloneCertificationInfo> skippedCertifications;

    /** Cảnh báo clone hiển thị trên giao diện (ví dụ chứng nhận bị bỏ qua). */
    private List<String> warnings;
}
