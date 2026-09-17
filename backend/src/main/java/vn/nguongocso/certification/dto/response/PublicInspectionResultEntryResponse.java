package vn.nguongocso.certification.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin tối thiểu của yêu cầu kiểm nghiệm trên cổng công khai.
 * Không làm lộ các ID nội bộ như organizationId, userId, lot UUID, hay request UUID.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicInspectionResultEntryResponse {

    /**
     * Tên đơn vị kiểm nghiệm được chỉ định theo đúng hợp đồng API.
     */
    private String testingUnitName;

    /**
     * Tên đơn vị kiểm nghiệm (alias tương thích ngược).
     */
    private String testingUnit;

    /**
     * Mã hiển thị của lô sản xuất.
     */
    private String lotCode;

    /**
     * Tên hiển thị của lô sản xuất.
     */
    private String lotName;

    /**
     * Ngày gửi mẫu đi kiểm nghiệm.
     */
    private LocalDate sampleSentDate;

    /**
     * Thời điểm hết hiệu lực của liên kết.
     */
    private LocalDateTime expiresAt;

    /**
     * Danh sách các chỉ tiêu kiểm nghiệm cần nhập kết quả.
     */
    private List<PublicInspectionResultEntryCriterionResponse> criteria;
}
