package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một LẦN kiểm nghiệm (round) của lô sản xuất trên trang tra cứu công khai.
 *
 * <p>
 * Mỗi lần gửi mẫu đi kiểm nghiệm (Inspection Request) tạo thành một phần tử
 * trong dòng thời gian lịch sử kiểm nghiệm. Các lần kiểm nghiệm lại
 * (re-inspection) được giữ nguyên trong lịch sử — kết quả không đạt của lần
 * trước KHÔNG bị xóa hay ghi đè.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionRoundDto {

    /**
     * Số thứ tự lần kiểm nghiệm, tính từ 1 cho lần cũ nhất.
     */
    private int round;

    /**
     * Tên phòng/đơn vị kiểm nghiệm thực hiện lần này.
     */
    private String laboratoryName;

    /**
     * Ngày gửi mẫu đi kiểm nghiệm.
     */
    private LocalDate sampleSentDate;

    /**
     * Trạng thái lần kiểm nghiệm:
     * PENDING | PASSED | FAILED | CANCELLED.
     */
    private String status;

    /**
     * Tổng số chỉ tiêu của lần kiểm nghiệm này.
     */
    private int totalCriteria;

    /**
     * Số chỉ tiêu đạt của lần này.
     */
    private int passedCriteria;

    /**
     * Số chỉ tiêu không đạt của lần này.
     */
    private int failedCriteriaCount;

    /**
     * Danh sách kết quả chi tiết các chỉ tiêu của lần này
     * (sắp xếp cũ đến mới).
     */
    private List<PublicInspectionCriterionResultDto> results;
}
