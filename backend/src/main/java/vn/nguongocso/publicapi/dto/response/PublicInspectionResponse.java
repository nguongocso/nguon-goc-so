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
     * Tổng số chỉ tiêu kiểm nghiệm đã công bố của lô.
     */
    private int totalCriteria;

    /**
     * Số chỉ tiêu đạt (passed = true).
     */
    private int passedCriteria;

    /**
     * Số chỉ tiêu không đạt (passed = false).
     * Kết quả hết hạn nhưng passed = true không được tính là không đạt.
     */
    private int failedCriteriaCount;

    /**
     * Tỷ lệ chỉ tiêu không đạt trên tổng số chỉ tiêu (%),
     * làm tròn 1 chữ số thập phân. Bằng 0.0 khi không có chỉ tiêu.
     */
    private double failedRatio;

    /**
     * Danh sách kết quả kiểm nghiệm các chỉ tiêu
     * (kết quả MỚI NHẤT của từng chỉ tiêu — trạng thái hiện tại).
     */
    private List<PublicInspectionCriterionResultDto> inspections;

    /**
     * Tổng số lần kiểm nghiệm đã thực hiện trên lô
     * (bao gồm cả kiểm nghiệm lần đầu và kiểm nghiệm lại).
     */
    private int roundCount;

    /**
     * Lịch sử kiểm nghiệm theo từng lần gửi mẫu, sắp xếp từ cũ đến mới.
     * Mỗi phần tử là một lần kiểm nghiệm kèm toàn bộ kết quả của lần đó —
     * các kết quả không đạt của lần trước vẫn được giữ nguyên.
     */
    private List<PublicInspectionRoundDto> history;
}
