package vn.nguongocso.publicapi.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response danh sách kết quả kiểm nghiệm của lô sản xuất trên trang tra cứu công khai. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionResponse {
    /** ID lô sản xuất. */
    private UUID productionLotId;

    /** Tên lô sản xuất. */
    private String lotName;

    /** Đánh dấu lô có kết quả kiểm nghiệm hay chưa. */
    private boolean hasInspection;

    /** Tổng số chỉ tiêu kiểm nghiệm. */
    private int totalCriteria;

    /** Số chỉ tiêu đạt. */
    private int passedCriteria;

    /** Số chỉ tiêu không đạt. */
    private int failedCriteriaCount;

    /** Tỷ lệ chỉ tiêu không đạt (%). */
    private double failedRatio;

    /** Danh sách kết quả kiểm nghiệm mới nhất của từng chỉ tiêu. */
    private List<PublicInspectionCriterionResultDto> inspections;

    /** Tổng số lần kiểm nghiệm đã thực hiện trên lô. */
    private int roundCount;

    /** Lịch sử kiểm nghiệm theo từng lần gửi mẫu. */
    private List<PublicInspectionRoundDto> history;
}
