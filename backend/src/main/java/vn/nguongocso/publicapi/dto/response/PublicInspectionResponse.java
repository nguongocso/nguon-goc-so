package vn.nguongocso.publicapi.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response danh sách kết quả kiểm nghiệm của lô sản xuất trên trang tra cứu công khai.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionResponse {

    /**
     * ID lô sản xuất.
     */
    private UUID productionLotId;

    /**
     * Tên lô sản xuất.
     */
    private String lotName;

    /**
     * Đánh dấu lô có kết quả kiểm nghiệm hay chưa.
     */
    private boolean hasInspection;

    /**
     * Danh sách kết quả kiểm nghiệm các chỉ tiêu.
     */
    private List<PublicInspectionCriterionResultDto> inspections;
}
