package vn.nguongocso.certification.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Lịch sử kiểm nghiệm của MỘT chỉ tiêu trên một lô sản xuất — bao gồm
 * dòng thời gian kết quả qua nhiều lần kiểm nghiệm / kiểm nghiệm lại.
 *
 * <p>
 * API trả về danh sách {@code CriterionHistoryResponse}, mỗi phần tử đại
 * diện cho một chỉ tiêu kèm toàn bộ lịch sử kết quả từ cũ đến mới.
 * </p>
 */
@Getter
@Setter
@Builder
public class CriterionHistoryResponse {

    /**
     * ID của chỉ tiêu (catalog criterion ID).
     */
    private Long criterionDefinitionId;

    /**
     * Mã chỉ tiêu (criterion code).
     */
    private String criterionCode;

    /**
     * Tên chỉ tiêu.
     */
    private String criterionName;

    /**
     * Lịch sử kết quả theo thứ tự thời gian từ cũ đến mới
     * (entry cuối cùng = kết quả hiện tại).
     */
    private List<CriterionHistoryEntry> history;
}
